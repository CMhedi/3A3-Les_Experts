package Services;

import java.util.EnumMap;
import java.util.Map;

public class WorkflowService {

    public enum State { DRAFT, PENDING, CONFIRMED, CANCELED, NO_SHOW }
    public enum Action { SUBMIT, CONFIRM, CANCEL, MARK_NO_SHOW, RESET }

    public static class TransitionResult {
        public final boolean allowed;
        public final State from;
        public final State to;
        public final String reason;

        public TransitionResult(boolean allowed, State from, State to, String reason) {
            this.allowed = allowed;
            this.from = from;
            this.to = to;
            this.reason = reason;
        }
    }

    private final Map<State, Map<Action, State>> fsm = new EnumMap<>(State.class);

    public WorkflowService() {
        put(State.DRAFT, Action.SUBMIT, State.PENDING);
        put(State.PENDING, Action.CONFIRM, State.CONFIRMED);
        put(State.PENDING, Action.CANCEL, State.CANCELED);
        put(State.CONFIRMED, Action.CANCEL, State.CANCELED);
        put(State.CONFIRMED, Action.MARK_NO_SHOW, State.NO_SHOW);
        put(State.CANCELED, Action.RESET, State.DRAFT);
    }

    private void put(State s, Action a, State next) {
        fsm.computeIfAbsent(s, k -> new EnumMap<>(Action.class)).put(a, next);
    }

    public TransitionResult transition(State current, Action action) {
        if (current == null || action == null) {
            return new TransitionResult(false, current, current, "INVALID_INPUT");
        }
        State next = fsm.getOrDefault(current, Map.of()).get(action);
        if (next == null) return new TransitionResult(false, current, current, "TRANSITION_NOT_ALLOWED");
        return new TransitionResult(true, current, next, "OK");
    }
}