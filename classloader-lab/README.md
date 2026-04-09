
## Stage-1
```
javac src/Stage1Inspector.java -d out/
java -cp out Stage1Inspector
```

You'll see `String` returns `null` (Bootstrap is native C++, invisible to Java), while your own class shows the full AppClassLoader → PlatformClassLoader → null chain.

## Stage-2
Compile it into the plugins/ folder — NOT your main classpath
```
javac plugins/Greeter.java -d plugins/
```

```
javac src/Stage2CustomLoader.java -d out/
java -cp out Stage2CustomLoader
```
