package nl.vpro.util;

import lombok.Getter;

/**
 * Implementation of {@link CommandExecutor} wrapping another one. The idea is that this can be
 * extended to add useful extra methods related to the wrapped command, while the command itself
 * can be instantiated with {@link CommandExecutorImpl#builder()}
 * @since 5.4
 */
@Getter
public class CommandExecutorWrapper extends AbstractCommandExecutorWrapper {

    final CommandExecutor wrapped;

    public CommandExecutorWrapper(CommandExecutor wrapped) {
        this.wrapped = wrapped;
    }
}
