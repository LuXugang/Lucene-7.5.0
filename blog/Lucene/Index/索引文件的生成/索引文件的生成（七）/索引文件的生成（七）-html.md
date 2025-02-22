---
title: 索引文件的生成（七）之tim&&tip（Lucene 7.5.0）
date: 2020-01-17 00:00:00
tags: [tim,tip]
categories:
- Lucene
- Index
---

&emsp;&emsp;本文承接[索引文件的生成（六）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0115/索引文件的生成（六）之tim&&tip)继续介绍剩余的内容，下面先给出生成索引文件.tim、.tip的流程图。

## 生成索引文件.tim、.tip的流程图

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/1.png">

### 统计每一个term的信息

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/2.png">

&emsp;&emsp;执行到该流程，我们需要将当前term的一些信息（图1中的IntBlockTermState，见文章[索引文件的生成（五）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0110/索引文件的生成（五）之tim&&tip)）的汇总到所属域的信息中（这里先提一下的是，这些信息在后面使用FieldMetaData封装），图2中出现的字段的含义如下：

- sumDocFreq：包含当前域的所有term的文档数量总和，注意的是当前域可能有多个term在同一文档中
- sumTotalTermFreq：当前域的所有term在所有文档中出现的次数总和
- numTerms：当前域中的term数量
- minTerm：当前域中最小（字典序）的term
- maxTerm：当前域中最大（字典序）的term

&emsp;&emsp;例如我们有如下几篇文档：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/3.png">

&emsp;&emsp;其中用<font color=Red>红色</font>标注的term属于域名为"content"的域，那么在处理完"content"之后，图2中的字段的值如下所示：

- sumDocFreq：b（2）+ c（3）+ f（1）+ h（1）= 7
- sumTotalTermFreq：b（3）+ c（3）+ f（1）+ h（1）= 8
- numTerms：b、c、f、h共4个term
- minTerm：b
- maxTerm：h

&emsp;&emsp;再处理完所有域之后，上述的信息在索引文件.tim中的位置如下：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/4.png">

### 生成NodeBlock

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/5.png">

&emsp;&emsp;当前域的所有term处理结束后，那么将term栈中剩余未处理的PendingEntry生成NodeBlock（见文章[索引文件的生成（六）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0115/索引文件的生成（六）之tim&&tip)）。

### 记录当前域的信息在.tip文件中的起始位置indexStartFP

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/6.png">

&emsp;&emsp;到此流程，Lucene将要在索引文件.tip中写入当前域的FSTIndex信息，在读取阶段，通过读取索引文件.tip中的FSTIndex信息来获取当前域在索引文件.tim的内容，而所有域的FSTIndex信息连续的存储在索引文件.tip中，那么需要indexStartFP来实现"索引"功能，如下图所示：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/7.png">

### 生成当前域的FSTIndex信息

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/8.png">

&emsp;&emsp;在图5的流程中，当前域的所有term处理结束后，term栈中剩余未处理的PendingEntry会被处理为NodeBlock，最终只会生成一个PendingBlock（没明白？见文章[索引文件的生成（六）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0115/索引文件的生成（六）之tim&&tip)），并且PendingBlock中的index信息，即FST信息将会被写入到FSTIndex中，数据结构如下：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/9.png">

&emsp;&emsp;在文章[FST算法（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/FST（一）)中我们说到，inputValues跟outputValues作为输入将被用于生成FST，在那篇文章的例子中，inputValues数组中每一个数组元素inputValue都不是"[ ]"，如果inputValues数组中第一个inputvalue为"[ ]"，由于它不能生成node，那么这种情况将被特殊处理，"[ ]"对应的outputValue将使用图9中的emptyOutputBtes字段存储，同时使用emptyLen字段描述emptyOutputBtes占用的字节数，在源码中，这种特殊情况被称为"empty string"、"empty input"。

#### 标志位

&emsp;&emsp;该字段的可取值为 0、1，用于描述FSTIndex两种不用的数据结构，使得在读取阶段能正确的读取字段值。

#### inputType

&emsp;&emsp;inputType可选值有三个，分别描述了label（lable的概念见文章[FST算法（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/FST（一）)）占用的字节数量：

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/10.png">

- BYTE1：一个字节
- BYTE2：两个字节
- BYTE4：四个字节

#### startNode

&emsp;&emsp;该字段描述的是在读取FST阶段，current数组中第一个读取的位置，在文章[FST算法（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/FST（一）)的例子中，startNode的值为40。

#### numBytes

&emsp;&emsp;该字段描述的是current数组占用的字节数。

#### bytes

&emsp;&emsp;该字段描述的是current数组中所有元素，也就是在文章[索引文件的生成（六）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Index/2020/0115/索引文件的生成（六）之tim&&tip)中所有PendingBlock中的prefix（除了prefix为"[ ]"的信息，它使用emptyOutputBytes存储）对应的FST的inputValue。


### 生成FieldMetaData

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/11.png">

&emsp;&emsp;由于Lucene的处理逻辑是先处理所有的域，最后把这些域的信息写入一次性到索引文件.tip中，故在处理完一个域后，要将该域的信息通过FieldMetaData来存储下，当所有的域处理结束后，遍历所有的FieldMetaData，将这些信息依次到索引文件.tip中，故有了图7中的数据结构，FieldMetaData中只有一个信息需要介绍下，其他信息可以自行看源码中https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-7.5.0/lucene/core/src/java/org/apache/lucene/codecs/blocktree/BlockTreeTermsWriter.java 的内部类FieldMetaData：

- docCount：该值描述的是包含当前域的文档号数量，以图3为例，三篇文档都包含了域名为"content"的文档，所以docCount = 3，该值是在生成索引文件.doc、pos、pay（见文章[索引文件的生成（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1226/索引文件的生成（一）之doc&&pay&&pos)）的过程中统计的，统计的时机点如下图<font color=Red>红框</font>标注的流程点：

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（七）/12.png">

&emsp;&emsp;至此，生成索引文件.tim、.tip的流程介绍完毕。

## 结语

&emsp;&emsp;相信看完这七篇的系列文章后，大家对于索引文件.doc、.pos、.pay、.tim、tip的生成以及他们之间的关系有了深刻的了解，自己品。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Index/索引文件的生成/索引文件的生成（七）/索引文件的生成（七）.zip)下载附件