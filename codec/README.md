# 说明

当前包下存储的是Netty对于各种编解码的支持；需要注意的是，这里存储的是对编解码的支持，而非各种协议的支持，Netty对于各种协议的支持主要看响应的`netty-codec-xxx`，比如`netty-codec-http`
是对`http`协议的支持，`netty-codec-redis`则是对`redis`协议的支持。

# 具体内容

当前项目下各目录内容：

* base64：BASE64 的支持，常用来把一个字符串转换成另一个字符串，简单加密
* bytes：ByteBuf 与 Java 本身的字节数组 byte[] 之间的互相转换
* compression：各种压缩协议的支持，比如 BZip、Snappy、Zlib 等
* json：通过 JSON 的形式来分割协议，不过，这里只有一个 JSON 一次解码器，因为 JSON 比较简单，只需要 toString () 就能拿到 JSON 文本了，所以，没有相应的二次编解码器，JSON
  的优点很多，跨语言，结构清晰，易读
* marshalling：JBoss 的 Marshalling 的支持，也是比较有名的，不过这里的实现没有很好地分层，通过源码可以看到 MarshallingEncoder 继承自 MessageToByteEncoder，而
  MarshallingDecoder 继承自 LengthFieldBasedFrameDecoder，缺少一种对称美
* protobuf：Google 的 Protobuf，因体积小，多语言支持而出名，而且不用写多少代码，只需要简单地定义好协议，使用工具一键生成 Java 对象，而且非常方便客户端与服务端不同语言的开发场景
* serialization：基于 Java 序列化做了一些优化，减小了序列化之后字节数组的大小，缺点很明显，只能 Java 中使用
* string：将 ByteBuf 转换成 Java 中的 String 对象，查看源码，其实很简单，只是调用 msg.toString (charset) 就完事了
* xml：XML 的支持，现在很少系统使用 XML 来传输数据了，缺点很明显，报文太大了