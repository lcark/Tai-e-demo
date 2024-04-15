package pascal.taie.analysis.extractapi;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.extractapi.pojo.MethodRouter;
import pascal.taie.analysis.extractapi.pojo.Router;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.annotation.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractApi extends ProgramAnalysis {

    public static  final String ID = "extractApi";
    public List<Router> routers = new ArrayList<>();
    public ExtractApi(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        World.get().getClassHierarchy().applicationClasses().forEach(jClass -> {
            AtomicReference<Boolean> flag = new AtomicReference<>(false);
            ArrayList<MethodRouter> methodRouters = new ArrayList<>();
            jClass.getDeclaredMethods().forEach(jMethod -> {
                //判断method是否有Mapping注解
                if (!jMethod.getAnnotations().stream().filter(
                        annotation -> annotation.getType().matches("org.springframework.web.bind.annotation.\\w+Mapping")
                ).toList().isEmpty()) {
                    flag.set(true);
                    //获取method的注解内容并添加进methodRouter类
                    MethodRouter methodRouter = new MethodRouter(jMethod.getName(), formatMappedPath(getPathFromAnnotation(jMethod.getAnnotations())));
                    methodRouters.add(methodRouter);
                }
            });
            if (flag.get()) {
                //获得class的注解并加入router里
                Router router = new Router(jClass.getName(), formatMappedPath(getPathFromAnnotation(jClass.getAnnotations())),methodRouters);
                routers.add(router);
            }
        });
        //将内容打印出来
        printPathFromRouters();
        return null;
    }

    public String getPathFromAnnotation(Collection<Annotation> annotations) {
        ArrayList<String> path = new ArrayList<>();
        annotations.stream()
                .filter(annotation -> annotation.getType().matches("org.springframework.web.bind.annotation.\\w+Mapping"))
                .forEach(annotation -> path.add(Objects.requireNonNull(annotation.getElement("value")).toString()));
        return path.size() == 1 ? path.get(0) : null;
    }
    public   void  printPathFromRouters(){
        routers.forEach(router -> {
//            System.out.println("class name:"+router.getClassName());
            List<String> completePathFromRouter = getCompletePathFromRouter(router);
            for (String path :completePathFromRouter){
                System.out.println(path);
            }
        });
    }
    public  List<String> getCompletePathFromRouter(Router router){
        ArrayList<String> routerList = new ArrayList<>();
        String classPath = router.classPath();

        router.methodRouters().forEach(methodRouter -> {
            String pathMethod = methodRouter.path();
            routerList.add(classPath+pathMethod);
        });
        return routerList;
    }
    public  String formatMappedPath(String originPath){
        String path=null;
        if (originPath==null){
            return "";
        }
        Pattern pattern = Pattern.compile("\\{\"(.*?)\"\\}");
        Matcher matcher = pattern.matcher(originPath);
        if (matcher.find()) {
            path = matcher.group(1); // Extract the text between curly braces
        }
        if(path ==null){
            return "";
        }
        //  /path/ => /path
        if (path.matches("/.*")&&path.matches(".*/")){
            return path.substring(0,path.length()-1);
        }
        // path/ => /path
        if (path.matches(".*/")&& !path.matches("/.*")){
            return "/"+path.substring(0,path.length()-1);
        }
        // path => /path
        if (!path.matches("/.*")&&!path.matches(".*/")){
            return "/"+path;
        }
        // /path => /path
        return path;
    }
}
