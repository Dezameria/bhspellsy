# Project Agent Rules

## Spell documentation is mandatory

These rules apply to every task that creates, changes, fixes, balances, refactors, or removes a spell.

1. Before changing spell code, read `docs/spells/README.md` and the spell-specific document at `docs/spells/<school>/<spell_id>.md`.
2. Treat each spell separately. Every spell must have its own document placed in its corresponding magic school directory and named with its registry path in lowercase snake case, for example `docs/spells/nature/wings_of_tempest.md`.
3. If a new spell has no document, create its spell-specific document as part of the same change. Do not implement a new spell without documenting it.
4. When implementation details change, update the corresponding spell document in the same change. This includes balance values, behavior, targeting, range, radius, duration, damage, effects, cooldown, mana cost, cast type/time, scaling, entities, particles, sounds, animations, assets, and server/client behavior.
5. If a task affects multiple spells, read and update each affected spell document independently. Do not combine their detailed specifications into one shared spell file.
6. Keep code, registry IDs, language entries, assets, and spell documentation consistent. If the existing document and code disagree, resolve the discrepancy according to the requested behavior and explicitly report the synchronization.
7. Before finishing a spell task, compare the final implementation against its document and run the relevant build or tests. Report any remaining mismatch or unverified runtime behavior.
8. `docs/all_spells.md` may remain a summary or index, but it does not replace the required per-spell document in `docs/spells/<school>/`.

