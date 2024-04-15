package pascal.taie.analysis.extractapi.pojo;

import java.util.List;

public record Router(String className,String classPath,List<MethodRouter> methodRouters){

    @Override
    public String toString() {
        return "Router{" +
                "className='" + className + '\'' +
                ", classPath='" + classPath + '\'' +
                ", methodRouters=" + methodRouters +
                '}';
    }
}
