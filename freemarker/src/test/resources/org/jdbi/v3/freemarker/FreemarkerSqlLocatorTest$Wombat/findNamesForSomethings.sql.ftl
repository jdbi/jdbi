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
<#include "org/jdbi/v3/freemarker/util.ftl">
<#include "util2.ftl">
select name from something where id in (<#list somethings as something>${something.id}<#sep>, </#list>) <@groupBy field="name" /> <@orderBy field="name" />