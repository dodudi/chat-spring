package kr.it.rudy.chat.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();
        String args = formatArgs(signature.getMethod(), joinPoint.getArgs());

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("[CONTROLLER] class={} method={} args={} elapsed={}ms", className, methodName, args, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.warn("[CONTROLLER_ERROR] class={} method={} args={} elapsed={}ms error={}", className, methodName, args, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    /**
     * args는 proceed() 이전에 캡처하므로 실행 중 객체 상태가 바뀌어도 진입 시점 값을 기록한다.
     * 인덱스 루프를 사용하는 이유: args[i]와 paramAnnotations[i]를 동시에 참조해야 하기 때문이다.
     */
    private String formatArgs(Method method, Object[] args) {
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        List<String> result = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (isFrameworkObject(args[i])) {
                continue;
            }
            result.add(isMasked(paramAnnotations[i]) ? "****" : String.valueOf(args[i]));
        }

        return result.toString();
    }

    /**
     * 프레임워크 내부 객체는 toString()이 장황하거나 순환 참조를 포함할 수 있어 제외한다.
     */
    private boolean isFrameworkObject(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof Model
                || arg instanceof BindingResult;
    }

    /** @see Masked */
    private boolean isMasked(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Masked) {
                return true;
            }
        }
        return false;
    }
}
