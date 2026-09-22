[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$PromptPath,

    [Parameter(Mandatory = $true)]
    [string]$WorkingDirectory,

    [ValidateSet("planning", "review")]
    [string]$Phase = "planning",

    [ValidateRange(5, 600)]
    [int]$StartupTimeoutSeconds = 45,

    [ValidateRange(15, 3600)]
    [int]$StallTimeoutSeconds = 180,

    [ValidateRange(30, 7200)]
    [int]$TotalTimeoutSeconds = 900,

    [string]$OutputDirectory,

    [switch]$IgnoreUserConfig
)

$ErrorActionPreference = "Stop"

function Get-FileLength {
    param([string]$Path)

    if (Test-Path -LiteralPath $Path) {
        return (Get-Item -LiteralPath $Path).Length
    }
    return 0L
}

function Get-DescendantProcessIds {
    param([int]$RootProcessId)

    $processes = @(Get-CimInstance Win32_Process)
    $pending = [System.Collections.Generic.Queue[int]]::new()
    $result = [System.Collections.Generic.List[int]]::new()
    $pending.Enqueue($RootProcessId)

    while ($pending.Count -gt 0) {
        $parentId = $pending.Dequeue()
        foreach ($child in $processes | Where-Object { $_.ParentProcessId -eq $parentId }) {
            $childId = [int]$child.ProcessId
            $result.Add($childId)
            $pending.Enqueue($childId)
        }
    }

    return @($result)
}

function Stop-ProcessTree {
    param([int]$RootProcessId)

    $descendants = @(Get-DescendantProcessIds -RootProcessId $RootProcessId)
    [array]::Reverse($descendants)
    foreach ($processId in @($descendants + $RootProcessId)) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

$resolvedPrompt = (Resolve-Path -LiteralPath $PromptPath).Path
$resolvedWorkingDirectory = (Resolve-Path -LiteralPath $WorkingDirectory).Path

if ([string]::IsNullOrWhiteSpace((Get-Content -Raw -LiteralPath $resolvedPrompt))) {
    throw "Prompt file is empty: $resolvedPrompt"
}

$codexCommand = @(Get-Command codex -CommandType Application -ErrorAction Stop)[0]
$codexPath = [string]$codexCommand.Source

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("codex-execution-" + [guid]::NewGuid().ToString("N"))
}
$null = New-Item -ItemType Directory -Path $OutputDirectory -Force
$resolvedOutputDirectory = (Resolve-Path -LiteralPath $OutputDirectory).Path

$tracePath = Join-Path $resolvedOutputDirectory "$Phase-trace.jsonl"
$errorPath = Join-Path $resolvedOutputDirectory "$Phase-stderr.log"
$lastMessagePath = Join-Path $resolvedOutputDirectory "$Phase-last-message.txt"

$quotedLastMessagePath = '"' + $lastMessagePath.Replace('"', '\"') + '"'
$arguments = @(
    "exec",
    "--json",
    "--ephemeral",
    "--color", "never",
    "--sandbox", "read-only",
    "--output-last-message", $quotedLastMessagePath
)
if ($IgnoreUserConfig) {
    $arguments += "--ignore-user-config"
}
$arguments += "-"

$startedAt = Get-Date
$lastActivityAt = $startedAt
$lastTraceLength = 0L
$lastErrorLength = 0L
$observedJsonEvent = $false
$terminalStatus = $null
$failureSummary = $null
$process = $null

try {
    $startParameters = @{
        FilePath = $codexPath
        ArgumentList = $arguments
        WorkingDirectory = $resolvedWorkingDirectory
        RedirectStandardInput = $resolvedPrompt
        RedirectStandardOutput = $tracePath
        RedirectStandardError = $errorPath
        WindowStyle = "Hidden"
        PassThru = $true
    }
    $process = Start-Process @startParameters

    while (-not $process.HasExited) {
        Start-Sleep -Milliseconds 500
        $process.Refresh()

        $now = Get-Date
        $traceLength = Get-FileLength -Path $tracePath
        $errorLength = Get-FileLength -Path $errorPath

        if ($traceLength -gt 0) {
            $observedJsonEvent = $true
        }
        if ($traceLength -ne $lastTraceLength -or $errorLength -ne $lastErrorLength) {
            $lastActivityAt = $now
            $lastTraceLength = $traceLength
            $lastErrorLength = $errorLength
        }

        if (-not $observedJsonEvent -and ($now - $startedAt).TotalSeconds -ge $StartupTimeoutSeconds) {
            $terminalStatus = "STARTUP_STALLED"
            $failureSummary = "Codex produced no JSONL event before the startup deadline."
            break
        }
        if ($observedJsonEvent -and ($now - $lastActivityAt).TotalSeconds -ge $StallTimeoutSeconds) {
            $terminalStatus = "STALLED"
            $failureSummary = "Codex produced no stdout/stderr activity before the stall deadline."
            break
        }
        if (($now - $startedAt).TotalSeconds -ge $TotalTimeoutSeconds) {
            $terminalStatus = "TIMED_OUT"
            $failureSummary = "Codex exceeded the total invocation deadline."
            break
        }
    }

    if ($null -ne $terminalStatus -and -not $process.HasExited) {
        Stop-ProcessTree -RootProcessId $process.Id
        $process.WaitForExit(5000) | Out-Null
    }

    $process.Refresh()
    if ($process.HasExited) {
        $process.WaitForExit()
    }
    $exitCode = if ($process.HasExited) {
        try { [int]$process.ExitCode } catch { $null }
    }
    else {
        $null
    }

    $lastEventType = $null
    $traceHasFailure = $false
    if (Test-Path -LiteralPath $tracePath) {
        foreach ($line in Get-Content -LiteralPath $tracePath) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            try {
                $event = $line | ConvertFrom-Json
                $lastEventType = $event.type
                if ($event.type -eq "error" -or $event.type -eq "turn.failed") {
                    $traceHasFailure = $true
                }
            }
            catch {
                $traceHasFailure = $true
                $lastEventType = "invalid_jsonl"
            }
        }
    }

    $hasLastMessage = (Test-Path -LiteralPath $lastMessagePath) -and
        -not [string]::IsNullOrWhiteSpace((Get-Content -Raw -LiteralPath $lastMessagePath))

    if ($null -eq $terminalStatus) {
        if (($null -ne $exitCode -and $exitCode -ne 0) -or $traceHasFailure) {
            $terminalStatus = "FAILED"
            $failureSummary = "Codex exited unsuccessfully or emitted a failure event."
        }
        elseif ($null -eq $exitCode -and $lastEventType -ne "turn.completed") {
            $terminalStatus = "FAILED"
            $failureSummary = "Codex exit code was unavailable and the trace did not complete successfully."
        }
        elseif (-not $hasLastMessage) {
            $terminalStatus = "INVALID_OUTPUT"
            $failureSummary = "Codex exited successfully but did not produce a non-empty final message."
        }
        else {
            $terminalStatus = "SUCCESS"
        }
    }

    $remainingTree = @()
    if ($null -ne $process) {
        $remainingTree = @(Get-DescendantProcessIds -RootProcessId $process.Id | Where-Object {
            Get-Process -Id $_ -ErrorAction SilentlyContinue
        })
    }
    $rootProcessAlive = $null -ne $process -and $null -ne (Get-Process -Id $process.Id -ErrorAction SilentlyContinue)

    $result = [ordered]@{
        Phase = $Phase
        Status = $terminalStatus
        ExitCode = $exitCode
        ProcessId = if ($null -ne $process) { $process.Id } else { $null }
        DurationSeconds = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 1)
        ObservedJsonEvent = $observedJsonEvent
        LastEventType = $lastEventType
        TracePath = $tracePath
        ErrorPath = $errorPath
        LastMessagePath = $lastMessagePath
        RootProcessAlive = $rootProcessAlive
        RemainingDescendantProcessIds = $remainingTree
        FailureSummary = $failureSummary
    }

    $result | ConvertTo-Json -Depth 4

    switch ($terminalStatus) {
        "SUCCESS" { exit 0 }
        "STARTUP_STALLED" { exit 2 }
        "STALLED" { exit 3 }
        "TIMED_OUT" { exit 4 }
        "FAILED" { exit 5 }
        default { exit 6 }
    }
}
catch {
    if ($null -ne $process -and -not $process.HasExited) {
        Stop-ProcessTree -RootProcessId $process.Id
    }
    [ordered]@{
        Phase = $Phase
        Status = "FAILED"
        ExitCode = $null
        TracePath = $tracePath
        ErrorPath = $errorPath
        LastMessagePath = $lastMessagePath
        FailureSummary = $_.Exception.Message
    } | ConvertTo-Json -Depth 4
    exit 5
}
