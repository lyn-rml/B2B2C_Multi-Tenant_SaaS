package com.marketplace.config;

import com.marketplace.auth.TenantContext;
import com.marketplace.auth.TenantContextHolder;
import com.marketplace.domain.RoleType;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class RlsDataSource extends DelegatingDataSource {

    public RlsDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyContext(connection);
        return wrap(connection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyContext(connection);
        return wrap(connection);
    }

    private Connection wrap(Connection target) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("close".equals(method.getName())) {
                    try {
                        resetContext(target);
                    } finally {
                        return method.invoke(target, args);
                    }
                }
                if ("getTargetConnection".equals(method.getName()) && method.getParameterCount() == 0) {
                    return target;
                }
                return method.invoke(target, args);
            }
        };
        return (Connection) Proxy.newProxyInstance(
                ConnectionProxy.class.getClassLoader(),
                new Class[]{ConnectionProxy.class},
                handler
        );
    }

    static void applyContext(Connection connection) throws SQLException {
        TenantContext context = TenantContextHolder.get();
        Long userId = context != null ? context.userId() : null;
        RoleType role = context != null ? context.activeRole() : null;
        UUID tenantId = context != null ? context.activeTenantId() : null;

        setConfig(connection, "app.user_id", userId != null ? String.valueOf(userId) : "");
        setConfig(connection, "app.active_role", role != null ? role.name() : "");
        setConfig(connection, "app.active_tenant", tenantId != null ? tenantId.toString() : "");
    }

    static void resetContext(Connection connection) throws SQLException {
        setConfig(connection, "app.user_id", "");
        setConfig(connection, "app.active_role", "");
        setConfig(connection, "app.active_tenant", "");
    }

    private static void setConfig(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT set_config(?, ?, false)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.execute();
        }
    }
}
