/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.lisp.msg.protocols;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default LISP map reply message class.
 */
public final class DefaultLispMapReply implements LispMapReply {

    private final long nonce;
    private final byte recordCount;
    private final boolean probe;
    private final boolean etr;
    private final boolean security;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce       nonce
     * @param recordCount record count number
     * @param probe       probe flag
     * @param etr         etr flag
     * @param security    security flag
     */
    private DefaultLispMapReply(long nonce, byte recordCount, boolean probe,
                                boolean etr, boolean security) {
        this.nonce = nonce;
        this.recordCount = recordCount;
        this.probe = probe;
        this.etr = etr;
        this.security = security;
    }

    @Override
    public LispType getType() {
        return LispType.LISP_MAP_REPLY;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {
        // TODO: serialize LispMapReply message
    }

    @Override
    public Builder createBuilder() {
        return new DefaultReplyBuilder();
    }

    @Override
    public boolean isProbe() {
        return this.probe;
    }

    @Override
    public boolean isEtr() {
        return this.etr;
    }

    @Override
    public boolean isSecurity() {
        return this.security;
    }

    @Override
    public byte getRecordCount() {
        return this.recordCount;
    }

    @Override
    public long getNonce() {
        return this.nonce;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("recordCount", recordCount)
                .add("probe", probe)
                .add("etr", etr)
                .add("security", security).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultLispMapReply that = (DefaultLispMapReply) o;
        return Objects.equal(nonce, that.nonce) &&
                Objects.equal(recordCount, that.recordCount) &&
                Objects.equal(probe, that.probe) &&
                Objects.equal(etr, that.etr) &&
                Objects.equal(security, that.security);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, recordCount, probe, etr, security);
    }

    public static final class DefaultReplyBuilder implements ReplyBuilder {

        private long nonce;
        private byte recordCount;
        private boolean probe;
        private boolean etr;
        private boolean security;

        @Override
        public LispType getType() {
            return LispType.LISP_MAP_REPLY;
        }

        @Override
        public ReplyBuilder withIsProbe(boolean probe) {
            this.probe = probe;
            return this;
        }

        @Override
        public ReplyBuilder withIsEtr(boolean etr) {
            this.etr = etr;
            return this;
        }

        @Override
        public ReplyBuilder withIsSecurity(boolean security) {
            this.security = security;
            return this;
        }

        @Override
        public ReplyBuilder withRecordCount(byte recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        @Override
        public ReplyBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public LispMapReply build() {
            return new DefaultLispMapReply(nonce, recordCount, probe, etr, security);
        }
    }
}
