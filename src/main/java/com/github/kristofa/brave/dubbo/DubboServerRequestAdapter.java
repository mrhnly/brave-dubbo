package com.github.kristofa.brave.dubbo;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.github.kristofa.brave.*;

import static com.github.kristofa.brave.IdConversion.convertToLong;


import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by chenjg on 16/7/24.
 */
public class DubboServerRequestAdapter  implements ServerRequestAdapter {

    private Invoker<?> invoker;
    private Invocation invocation;
    private ServerTracer serverTracer;

    public DubboServerRequestAdapter(Invoker<?> invoker, Invocation invocation,ServerTracer serverTracer) {
        this.invoker = invoker;
        this.invocation = invocation;
        this.serverTracer = serverTracer;
    }

    @Override
    public TraceData getTraceData() {
      String sampled =   invocation.getAttachment("sampled");
      if(sampled != null && sampled.equals("0")){
          return TraceData.builder().sample(false).build();
      }else {
          final String parentId = invocation.getAttachment("parentId");
          final String spanId = invocation.getAttachment("spanId");
          final String traceId = invocation.getAttachment("traceId");
          if (traceId != null && spanId != null) {
              SpanId span = getSpanId(traceId, spanId, parentId);
              return TraceData.builder().sample(true).spanId(span).build();
          }
      }
       return TraceData.builder().build();

    }

    @Override
    public String getSpanName() {
        return invoker.getInterface().getSimpleName()+"."+invocation.getMethodName();
    }

    @Override
    public Collection<KeyValueAnnotation> requestAnnotations() {

        String application = RpcContext.getContext().getUrl().getParameter("application");
        String ipAddr = RpcContext.getContext().getUrl().getIp();
        InetSocketAddress inetSocketAddress = RpcContext.getContext().getRemoteAddress();
        serverTracer.setServerReceived(IPConversion.convertToInt(ipAddr),inetSocketAddress.getPort(),"rpc");

        InetSocketAddress socketAddress = RpcContext.getContext().getLocalAddress();
        if (socketAddress != null) {
            KeyValueAnnotation remoteAddrAnnotation = KeyValueAnnotation.create(
                   "address", socketAddress.toString());
            return Collections.singleton(remoteAddrAnnotation);
        } else {
            return Collections.emptyList();
        }

    }

    static SpanId getSpanId(String traceId, String spanId, String parentSpanId) {
        return SpanId.builder()
                .traceId(convertToLong(traceId))
                .spanId(convertToLong(spanId))
                .parentId(parentSpanId == null ? null : convertToLong(parentSpanId)).build();
    }


}
