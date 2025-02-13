package org.infinispan.server.resp.commands.json;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.json.AppendType;
import org.infinispan.server.resp.json.EmbeddedJsonCache;
import org.infinispan.server.resp.json.JSONUtil;

import io.netty.channel.ChannelHandlerContext;

/**
 * JSON.STRAPPEND
 *
 * @see <a href="https://redis.io/commands/json.strappend/">JSON.STRAPPEND</a>
 * @since 15.2
 */
public class JSONSTRAPPEND extends JSONAPPEND {
    public static String STR_TYPE_NAME = AppendType.STRING.name().toLowerCase();
    public JSONSTRAPPEND() {
        super("JSON.STRAPPEND", -3, 1, 1, 1);
    }

    @Override
    public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
                                                       List<byte[]> arguments) {
        byte[] key = arguments.get(0);
        byte[] path = JSONUtil.DEFAULT_PATH;
        byte[] value = arguments.get(1);
        if (arguments.size() > 2) {
            path = arguments.get(1);
            value = arguments.get(2);
        }
        // To keep compatibility, considering the first path only. Additional args will
        // be ignored
        // If missing, default path '.' is used, it's in legacy style, i.e. not jsonpath
        byte[] jsonPath = JSONUtil.toJsonPath(path);
        boolean withPath = path == jsonPath;
        EmbeddedJsonCache ejc = handler.getJsonCache();
        CompletionStage<List<Long>> lengths = ejc.strAppend(key, jsonPath, value);
        return returnResult(handler, ctx, jsonPath, withPath, lengths);
    }

    @Override
    protected String getOpType() {
        return STR_TYPE_NAME;
    }

}
