/* Copyright 2004-2005 Brian McCallister
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

package org.skife.jdbi.spring;

import org.skife.jdbi.DBIException;
import org.skife.jdbi.Handle;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * Implementation of an AOP advice that provides translation
 * of SQLExceptions reported by DBI into Spring's DataAccessException hierarchy.
 *
 * @author Thomas Risberg
 */
public class SQLExceptionTranslatingThrowsAdvice implements ThrowsAdvice
{

    private SQLExceptionTranslator exceptionTranslator;

    public SQLExceptionTranslatingThrowsAdvice(DataSource dataSource)
    {
        this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    public void afterThrowing(Method method, Object[] args, Object target, DBIException ex) throws Throwable
    {
        Throwable t = ex.getCause();

        // trisberg: we really want the failing SQL statement here - is that possible??
        // not easy right now, will see what I can do --bmc
        String sql = ((Handle) target).toString();
        if (t instanceof SQLException)
        {
            throw exceptionTranslator.translate("DBI", sql, (SQLException) t);
        }
        else
        {
            throw new DataRetrievalFailureException("DBI Exception:" + ex.getMessage(), t);
        }
    }
}
