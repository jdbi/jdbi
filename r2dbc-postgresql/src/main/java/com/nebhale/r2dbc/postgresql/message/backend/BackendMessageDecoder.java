/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.r2dbc.postgresql.message.backend;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageUtils.getBody;
import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageUtils.getEnvelope;

/**
 * A decoder that reads {@link ByteBuf}s and returns a {@link Flux} of decoded {@link BackendMessage}s.
 */
public final class BackendMessageDecoder {

    private final AtomicReference<ByteBuf> remainder = new AtomicReference<>();

    /**
     * Decode a {@link ByteBuf} into a {@link Flux} of {@link BackendMessage}s.  If the {@link ByteBuf} does not end on a {@link BackendMessage} boundary, the {@link ByteBuf} will be retained until
     * an the concatenated contents of all retained {@link ByteBuf}s is a {@link BackendMessage} boundary.
     *
     * @param in the {@link ByteBuf} to decode
     * @return a {@link Flux} of {@link BackendMessage}s
     */
    public Flux<BackendMessage> decode(ByteBuf in) {
        Objects.requireNonNull(in, "in must not be null");

        return Flux.generate(
            () -> {
                ByteBuf remainder = this.remainder.getAndSet(null);
                return remainder == null ? in : Unpooled.wrappedBuffer(remainder, in);
            },
            (byteBuf, sink) -> {
                ByteBuf envelope = getEnvelope(byteBuf);

                if (envelope == null) {
                    if (byteBuf.readableBytes() > 0) {
                        this.remainder.set(byteBuf.retain());
                    }

                    sink.complete();
                    return byteBuf;
                }

                MessageType messageType = MessageType.valueOf(envelope.readByte());
                ByteBuf body = getBody(envelope);

                switch (messageType) {
                    case AUTHENTICATION:
                        decodeAuthentication(body, sink);
                        break;
                    case BACKEND_KEY_DATA:
                        sink.next(BackendKeyData.decode(body));
                        break;
                    case BIND_COMPLETE:
                        sink.next(BindComplete.INSTANCE);
                        break;
                    case CLOSE_COMPLETE:
                        sink.next(CloseComplete.INSTANCE);
                        break;
                    case COMMAND_COMPLETE:
                        sink.next(CommandComplete.decode(body));
                        break;
                    case COPY_DATA:
                        sink.next(CopyData.decode(body));
                        break;
                    case COPY_DONE:
                        sink.next(CopyDone.INSTANCE);
                        break;
                    case COPY_BOTH_RESPONSE:
                        sink.next(CopyBothResponse.decode(body));
                        break;
                    case COPY_IN_RESPONSE:
                        sink.next(CopyInResponse.decode(body));
                        break;
                    case COPY_OUT_RESPONSE:
                        sink.next(CopyOutResponse.decode(body));
                        break;
                    case DATA_ROW:
                        sink.next(DataRow.decode(body));
                        break;
                    case EMPTY_QUERY_RESPONSE:
                        sink.next(EmptyQueryResponse.INSTANCE);
                        break;
                    case ERROR_RESPONSE:
                        sink.next(ErrorResponse.decode(body));
                        break;
                    case FUNCTION_CALL_RESPONSE:
                        sink.next(FunctionCallResponse.decode(body));
                        break;
                    case NO_DATA:
                        sink.next(NoData.INSTANCE);
                        break;
                    case NOTICE_RESPONSE:
                        sink.next(NoticeResponse.decode(body));
                        break;
                    case NOTIFICATION_RESPONSE:
                        sink.next(NotificationResponse.decode(body));
                        break;
                    case PARAMETER_DESCRIPTION:
                        sink.next(ParameterDescription.decode(body));
                        break;
                    case PARAMETER_STATUS:
                        sink.next(ParameterStatus.decode(body));
                        break;
                    case PARSE_COMPLETE:
                        sink.next(ParseComplete.INSTANCE);
                        break;
                    case PORTAL_SUSPENDED:
                        sink.next(PortalSuspended.INSTANCE);
                        break;
                    case READY_FOR_QUERY:
                        sink.next(ReadyForQuery.decode(body));
                        break;
                    case ROW_DESCRIPTION:
                        sink.next(RowDescription.decode(body));
                        break;
                    default:
                        sink.error(new IllegalArgumentException(String.format("%s is not a supported message type", messageType)));
                }

                return byteBuf;
            },
            ReferenceCountUtil::release);

    }

    private static void decodeAuthentication(ByteBuf in, SynchronousSink<BackendMessage> sink) {
        AuthenticationType authenticationType = AuthenticationType.valueOf(in.readInt());

        switch (authenticationType) {
            case OK:
                sink.next(AuthenticationOk.INSTANCE);
                break;
            case KERBEROS_V5:
                sink.next(AuthenticationKerberosV5.INSTANCE);
                break;
            case CLEARTEXT_PASSWORD:
                sink.next(AuthenticationCleartextPassword.INSTANCE);
                break;
            case GSS:
                sink.next(AuthenticationGSS.INSTANCE);
                break;
            case GSS_CONTINUE:
                sink.next(AuthenticationGSSContinue.decode(in));
                break;
            case MD5_PASSWORD:
                sink.next(AuthenticationMD5Password.decode(in));
                break;
            case SCMC_CREDENTIAL:
                sink.next(AuthenticationSCMCredential.INSTANCE);
                break;
            case SASL:
                sink.next(AuthenticationSASL.decode(in));
                break;
            case SASL_CONTINUE:
                sink.next(AuthenticationSASLContinue.decode(in));
                break;
            case SASL_FINAL:
                sink.next(AuthenticationSASLFinal.decode(in));
                break;
            case SSPI:
                sink.next(AuthenticationSSPI.INSTANCE);
                break;
            default:
                sink.error(new IllegalArgumentException(String.format("%s is not a supported authentication type", authenticationType)));
        }
    }

    private enum AuthenticationType {

        OK(0),
        KERBEROS_V5(2),
        CLEARTEXT_PASSWORD(3),
        GSS(7),
        GSS_CONTINUE(8),
        MD5_PASSWORD(5),
        SCMC_CREDENTIAL(6),
        SASL(10),
        SASL_CONTINUE(11),
        SASL_FINAL(12),
        SSPI(9);

        private final int discriminator;

        AuthenticationType(int discriminator) {
            this.discriminator = discriminator;
        }

        static AuthenticationType valueOf(int i) {
            return Arrays.stream(values())
                .filter(type -> type.discriminator == i)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("%d is not a valid authentication type", i)));
        }

    }

    private enum MessageType {

        AUTHENTICATION('R'),
        BACKEND_KEY_DATA('K'),
        BIND_COMPLETE('2'),
        CLOSE_COMPLETE('3'),
        COMMAND_COMPLETE('C'),
        COPY_BOTH_RESPONSE('W'),
        COPY_DATA('d'),
        COPY_DONE('c'),
        COPY_IN_RESPONSE('G'),
        COPY_OUT_RESPONSE('H'),
        DATA_ROW('D'),
        EMPTY_QUERY_RESPONSE('I'),
        ERROR_RESPONSE('E'),
        FUNCTION_CALL_RESPONSE('V'),
        NO_DATA('n'),
        NOTICE_RESPONSE('N'),
        NOTIFICATION_RESPONSE('A'),
        PARAMETER_DESCRIPTION('t'),
        PARAMETER_STATUS('S'),
        PARSE_COMPLETE('1'),
        PORTAL_SUSPENDED('s'),
        READY_FOR_QUERY('Z'),
        ROW_DESCRIPTION('T');

        private final char discriminator;

        MessageType(char discriminator) {
            this.discriminator = discriminator;
        }

        static MessageType valueOf(byte b) {
            return Arrays.stream(values())
                .filter(type -> type.discriminator == b)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("%c is not a valid message type", b)));
        }

    }

}
