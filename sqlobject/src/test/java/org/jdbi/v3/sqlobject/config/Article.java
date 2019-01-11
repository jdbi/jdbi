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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

@Data
@NoArgsConstructor
public class Article {
    private long id;
    private String title;
    private String content;
    private List<Comment> comments = new ArrayList<>();

    @JdbiConstructor
    public Article(long id, String title, String content) {
        setId(id);
        setTitle(title);
        setContent(content);
    }

    public static Article newArticle(long id, String title) {
        return newArticle(id, title, null);
    }

    public static Article newArticle(long id, String title, String content, Comment... comments) {
        Article article = new Article(id, title, content);

        article.getComments().addAll(Arrays.asList(comments));

        return article;
    }
}
