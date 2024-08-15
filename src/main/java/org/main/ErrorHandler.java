package org.main;

import java.util.List;
import java.util.regex.*;

// 错误信息的父类
abstract class CompilationError {
    public abstract boolean handle();
//    public abstract boolean recover();
}

// 错误处理类
public class ErrorHandler {
    //用来一次性处理多个错误的
    public static void handleError(List<? extends CompilationError> errors) {
        for (CompilationError error : errors) {
            error.handle();
        }
    }

}
