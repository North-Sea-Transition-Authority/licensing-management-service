package uk.co.nstauthority.licensingmanagementservice.configuration;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
class TracingAspect {

  private final Tracer tracer;

  TracingAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  @Pointcut("@within(org.springframework.stereotype.Controller)" +
      " || @within(org.springframework.web.bind.annotation.RestController)" +
      " || @within(org.springframework.stereotype.Service)" +
      " || within(org.springframework.data.repository.Repository+)")
  void applicationLayer() {
  }

  @Around("applicationLayer()")
  public Object traceApplicationLayer(ProceedingJoinPoint joinPoint) throws Throwable {
    if (tracer.currentSpan() == null) {
      return joinPoint.proceed();
    }

    String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
    String methodName = joinPoint.getSignature().getName();

    Span span = tracer.nextSpan()
        .name(className + "." + methodName)
        .tag("class", className)
        .tag("method", methodName)
        .start();
    try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
      return joinPoint.proceed();
    } catch (Throwable ex) {
      span.error(ex);
      throw ex;
    } finally {
      span.end();
    }
  }

}
