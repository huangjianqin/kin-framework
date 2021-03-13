package org.kin.framework.statemachine;


/**
 * Created by 健勤 on 2017/8/11.
 */
public class StateMachineTest {
    public static void main(String[] args) {
//        Impl impl = new Impl();
//        StateMachineFactory<Impl, NumberState, FirstEventType, FirstEvent> factory
//                = new StateMachineFactory<Impl, NumberState, FirstEventType, FirstEvent>(NumberState.ONE)
//                .addTransition(NumberState.ONE, NumberState.TWO, FirstEventType.O, new NumberTransition())
//                .addTransition(NumberState.TWO, EnumSet.of(NumberState.THREE, NumberState.FOUR, NumberState.FIVE), FirstEventType.N, new NumberMoreTransition())
//                .installTopology();
//        StateMachine<NumberState, FirstEventType, FirstEvent> stateMachine = factory.make(impl);
//        System.out.println(stateMachine.getCurrentState());
//        stateMachine.doTransition(FirstEventType.O, new FirstEvent(FirstEventType.O));
//        System.out.println(stateMachine.getCurrentState());
//        stateMachine.doTransition(FirstEventType.N, new FirstEvent(FirstEventType.N));
//        System.out.println(stateMachine.getCurrentState());
//        impl.close();
    }
}

//class NumberMoreTransition implements MultipleArcTransition<Impl, FirstEvent, NumberState> {
//
//    @Override
//    public NumberState transition(Impl impl, FirstEvent firstEvent) {
//        return impl.handle2(firstEvent);
//    }
//}
//
//class NumberTransition implements SingleArcTransition<Impl, FirstEvent> {
//
//    @Override
//    public void transition(Impl impl, FirstEvent firstEvent) {
//        impl.handle(firstEvent);
//    }
//}

//class Impl implements EventHandler<FirstEvent> {
//    private DefaultEventDispatcher dispatcher;
//
//    public Impl() {
//        this.dispatcher = new DefaultEventDispatcher();
//        dispatcher.register(FirstEvent.class, new FirstEventHandler(), FirstEventHandler.class.getMethods()[0]);
//    }
//
//    @Override
//    public void handle(FirstEvent event) {
//        dispatcher.dispatch(event);
//    }
//
//    public NumberState handle2(FirstEvent event) {
//        dispatcher.dispatch(event);
//        return (NumberState) EnumSet.of(NumberState.THREE, NumberState.FOUR, NumberState.FIVE).toArray()[new Random().nextInt(3)];
//    }
//
//    public void close() {
//        dispatcher.shutdown();
//    }
//}
//
//class FirstEventHandler implements EventHandler<FirstEvent> {
//
//    @Override
//    public void handle(FirstEvent event) {
//        System.out.println("handle " + event);
//    }
//}