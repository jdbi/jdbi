<#--

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
SELECT
    *
FROM
    users u
WHERE
    1 = 1
<#if pageToken??>
        <#if pageToken.direction == "FORWARD">
            AND u.user_id < :pageToken.userId
        <#else>
            AND u.user_id > :pageToken.userId
        </#if>
</#if>
ORDER BY
<#if !pageToken?? || pageToken.direction == "FORWARD">
    u.user_id ASC
<#else>
    u.user_id DESC
</#if>
LIMIT
    :limit
