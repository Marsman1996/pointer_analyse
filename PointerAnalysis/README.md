# 程序分析大作业
本项目实现了一个基于Soot的数据流分析的指针分析程序.

## 小组成员

## 代码结构
`src/MyPointerAnalysis.java`为分析程序入口  
`src/WholeProgramTransformer.java`为程序的soot转换部分  
`src/AnswerPrinter.java`用于程序输出分析结果到文件  
`src/ObjectInfo.java`定义用于保存变量的信息及一些操作方法的类  
`src/PointToSet.java`定义数据流分析的元素及操作方法的类  
`src/MyForwardFlow.java`具体实现的数据流分析算法  

## 设计思想
1. 流敏感分析: 本项目基于Soot中的前向流分析框架实现了对指针指向集的流敏感分析, 主要内容是需要重写及实现merge/copy/flowThrough等方法; 定义了自己的指向集类PointToSet, 主要需要定义equals和hashcode两个方法, 以在前向流分析框架中比对前后指向集是否相同, 计算是否达到不动点.
2. 域敏感分析: 针对域赋值和数组赋值等操作。在非域敏感分析中, 对象及对象中的域会被视为一个整体, 分析它们所指的内容, 而在域敏感分析中, 需要将两者区分开来。在本项目的实现中, flowThrough在遇到域相关的指令时, 会获取base和field, 然后分析其具体指向集的变化.
3. 过程间分析: 过程间分析主要涉及到函数调用, 需要将指向集传递给方法的形参, 如果是special invoke指令, 还需要将this的信息添加到指向集中, 然后进入到新方法中进行分析, 分析结束后, 需要将改动的内容以及return指令返回的结果记录。我们通过在ObjectInfo中的type属性变量的类型, 2：普通局部变量 1: new出来的对象 0:未知 -3：需要返回的变量 -2: 参数 -1: this, 通过type来记录需要返回的结果; 同时维护一个函数调用栈`callstack`, 分析被调用函数时将其入栈, 分析结束后出栈. 当发生递归调用时, 即`callstack`出现重复, 跳过对递归函数的分析.

## 使用命令和测试示例
该分析程序使用命令为: `java -jar analyzer.jar [代码位置] [类名]`  
以`/code`中测试为例, 执行命令`java -jar analyzer.jar code\  test.Test5`后, result.txt中输出为:
```
1: 1
2: 2
3: 2
4: 3
```