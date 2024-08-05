package org.main;

import java.util.List;
import java.util.regex.*;

// 错误信息的父类
abstract class CompilationError {
    public abstract void handle();
}

// 错误处理类
public class ErrorHandler {
    public static void handleError(List<? extends CompilationError> errors) {
        for (CompilationError error : errors) {
            error.handle();
        }
    }
}
