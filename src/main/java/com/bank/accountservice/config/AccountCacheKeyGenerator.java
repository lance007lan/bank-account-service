package com.bank.accountservice.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

// Cache key format: <methodName>::an=<accountNumber>|cn=<customerName>|nn=<accountNickname>
// Absent values use "_". Example: getAccounts::an=_|cn=Alice Smith|nn=_
@Component("accountCacheKeyGenerator")
public class AccountCacheKeyGenerator implements KeyGenerator {

    enum Param { an, cn, nn, lm }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String an = params[0] != null ? params[0].toString() : "_";
        String cn = params[1] != null ? params[1].toString() : "_";
        String nn = params[2] != null ? params[2].toString() : "_";
        String limit = params[3] != null ? params[3].toString() : "_";
        return method.getName() + "::"
                + Param.an + "=" + an + "|"
                + Param.cn + "=" + cn + "|"
                + Param.nn + "=" + nn + "|"
                + Param.lm + "=" + limit;
    }
}
