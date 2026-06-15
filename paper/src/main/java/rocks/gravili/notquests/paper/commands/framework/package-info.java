/**
 * NotQuests' own command framework, built directly on Paper's native Brigadier command API
 * ({@code io.papermc.paper.command.brigadier}) and registered through Paper's
 * {@code LifecycleEvents.COMMANDS} lifecycle event.
 *
 * <p>This package exists so NotQuests can depend on first-party, always-maintained Paper APIs
 * instead of the third-party Cloud command framework (whose Paper integration has only ever shipped
 * pre-release/beta builds). The guiding principle is to use Paper's {@code Commands} API directly and
 * build thin wrappers here only <b>where necessary</b>:
 *
 * <ul>
 *   <li>{@link rocks.gravili.notquests.paper.commands.framework.NQArgumentType} — base for custom
 *       argument types (replaces Cloud's {@code ArgumentParser}/{@code ParserDescriptor}).</li>
 *   <li>{@link rocks.gravili.notquests.paper.commands.framework.NQCommands} — registrar that hooks
 *       the Paper {@code COMMANDS} lifecycle event and registers our Brigadier command trees.</li>
 * </ul>
 *
 * <p><b>Migration status:</b> foundation + a self-test command ({@code /nqnative}) are live and
 * coexist with the Cloud-registered commands on the same Brigadier dispatcher. The Cloud command
 * framework is removed only once this package reaches feature parity (builder ergonomics, all 19
 * argument parsers ported, flags, help menu, exception/usage handling).
 */
package rocks.gravili.notquests.paper.commands.framework;
