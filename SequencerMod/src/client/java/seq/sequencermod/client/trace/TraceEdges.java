package seq.sequencermod.client.trace;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

// Аспект, который пишет ребра вызовов внутри пакета seq.sequencermod.client..*
@Aspect
public class TraceEdges {

    // Все методы в вашем клиентском коде, исключая сам пакет .trace
    @Pointcut("execution(* seq.sequencermod.client..*(..)) && !within(seq.sequencermod.client.trace..*)")
    public void clientMethods() {}

    @Before("clientMethods()")
    public void onEnter(JoinPoint jp) {
        if (!(TraceCfg.ENABLED && TraceCfg.TRACE_EDGES)) return;
        String cls = jp.getSignature().getDeclaringTypeName();
        String mtd = jp.getSignature().getName();
        CallGraphTrace.onEnter(cls, mtd, jp.getArgs());
    }

    @After("clientMethods()")
    public void onExit(JoinPoint jp) {
        if (!(TraceCfg.ENABLED && TraceCfg.TRACE_EDGES)) return;
        String cls = jp.getSignature().getDeclaringTypeName();
        String mtd = jp.getSignature().getName();
        CallGraphTrace.onExit(cls, mtd);
    }
}