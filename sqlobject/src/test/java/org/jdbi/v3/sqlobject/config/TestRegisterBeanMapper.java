/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.sqlobject.config.Article.newArticle;
import static org.jdbi.v3.sqlobject.config.Comment.newComment;

import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisterBeanMapper {
    @Rule
    public H2DatabaseRule rule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    Handle handle;

    @Before
    public void setUp() {
        handle = rule.getSharedHandle();

        handle.execute("create table articles (" +
                "id      integer not null, " +
                "title   varchar not null, " +
                "content varchar not null)");
        handle.execute("create table comments (" +
                "id         integer not null, " +
                "article_id integer not null, " +
                "content    varchar not null)");

        handle.execute("insert into articles (id, title, content) values (?, ?, ?)", 1, "title 1", "content 1");
        handle.execute("insert into articles (id, title, content) values (?, ?, ?)", 2, "title 2", "content 2");

        handle.execute("insert into comments (id, article_id, content) values (?, ?, ?)", 10, 1, "comment 10");
        handle.execute("insert into comments (id, article_id, content) values (?, ?, ?)", 11, 1, "comment 11");
        handle.execute("insert into comments (id, article_id, content) values (?, ?, ?)", 20, 2, "comment 20");
    }

    @Test
    public void registerBeanMappers() {
        BlogDao dao = handle.attach(BlogDao.class);

        assertThat(dao.listArticleSummaries()).containsExactly(
                newArticle(1, "title 1"),
                newArticle(2, "title 2"));

        assertThat(dao.getArticleWithComments(0)).isEmpty();
        assertThat(dao.getArticleWithComments(1)).contains(
                newArticle(1, "title 1", "content 1", newComment(10, "comment 10"), newComment(11, "comment 11")));
        assertThat(dao.getArticleWithComments(2)).contains(
                newArticle(2, "title 2", "content 2", newComment(20, "comment 20")));
    }

    public interface BlogDao extends SqlObject {
        @SqlQuery("select id, title from articles order by id")
        @RegisterBeanMapper(Article.class)
        List<Article> listArticleSummaries();

        @RegisterBeanMapper(value = Article.class, prefix = "a")
        @RegisterBeanMapper(value = Comment.class, prefix = "c")
        default Optional<Article> getArticleWithComments(long id) {
            return getHandle().select(
                    "select " +
                            "  a.id      a_id, " +
                            "  a.title   a_title, " +
                            "  a.content a_content, " +
                            "  c.id      c_id, " +
                            "  c.content c_content " +
                            "from articles a " +
                            "left join comments c " +
                            "  on a.id = c.article_id " +
                            "where a.id = ? " +
                            "order by c.id",
                    id)
                    .reduceRows(Optional.<Article>empty(),
                            (acc, rv) -> {
                                Article a = acc.orElseGet(() -> rv.getRow(Article.class));

                                if (rv.getColumn("c_id", Long.class) != null) {
                                    a.getComments().add(rv.getRow(Comment.class));
                                }

                                return Optional.of(a);
                            });
        }
    }

}
