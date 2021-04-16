# Dhjar-bugfix

## 简介

Dhjar 是一款可以捕获第三方jar库异常的android gradle插件,通过配置实现在第三方jar方法上包裹try catch,主要是为了屏蔽一些对流程没影响的小错误

本地导入方式参考[dhmethod](https://github.com/dikeboy/DhMethodTime)

##  怎么使用

jcenter地址   https://bintray.com/dikeboy/dhjar/jarcatch

在项目build.gradle 加入
```python
classpath 'com.lin.dhjar:dhjar:1.0.0
```
App build.gradle 
```python
apply plugin: "dhjarplugin"
...
...
dhjar {
    jarFiles("appcompat","test")
    cutFiles("com.vova.testlibrary.TestFile:getInt:getFloat:getDouble:getLong:getShort:getChar:getByte:getString")
}
```
jarFiles:需要拦截的三方库名称
cutFiles:类和方法 "class:method:method","class:method:method" 分隔


### 参考
* [Javassist 介绍](http://www.javassist.org/tutorial/tutorial.html)
* [插件介绍](https://www.cnblogs.com/dikeboy/p/11505800.html)

