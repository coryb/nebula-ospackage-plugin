/*
 * Copyright 2014 Netflix, Inc.
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
package nebula.plugin.ospackage.daemon

import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import nebula.test.PluginProjectSpec
import org.codehaus.groovy.runtime.StackTraceUtils

class OspackageDaemonPluginSpec extends PluginProjectSpec {
    @Override
    String getPluginName() {
        'nebula-ospackage-daemon'
    }

    def 'at least one deamonName is needed'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            command = 'exit 0'
        }

        then:
        noExceptionThrown()

        when:
        plugin.extension.daemon {
            command = 'exit 0'
        }
        project.evaluate()

        then:
        def e = thrown(Exception)
        StackTraceUtils.extractRootCause(e) instanceof IllegalArgumentException
    }

    def 'no duplicate names'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            daemonName = 'foo'
        }
        plugin.extension.daemon {
            daemonName = 'foo'
        }
        project.evaluate()

        then:
        def e = thrown(Exception)
        StackTraceUtils.extractRootCause(e) instanceof IllegalArgumentException
    }

    def 'can call daemon directly in project'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        DaemonExtension extension = plugin.extension

        project.daemon {
            daemonName = 'foobar'
            command = 'exit 0'
        }

        then: 'daemon was added to list'
        !extension.daemons.isEmpty()

        then: 'daemon configurate'
        extension.daemons.iterator().next().daemonName == 'foobar'
    }

    def 'can call daemons extensions in project'() {
        when:
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        DaemonExtension extension = plugin.extension

        project.daemons {
            daemon {
                daemonName = 'foobar'
                command = 'exit 0'
            }
        }

        then: 'daemon was added to list'
        !extension.daemons.isEmpty()

        then: 'daemon configurate'
        extension.daemons.iterator().next().daemonName == 'foobar'
    }

    def 'tasks are created'() {
        when:
        project.plugins.apply(SystemPackagingPlugin)
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            daemonName = 'foobar'
            command = 'exit 1'
        }
        plugin.extension.daemon {
            daemonName = 'baz'
            command = 'exit 0'
        }
        project.evaluate()

        then:
        project.tasks.getByName('buildDebFoobarDaemon')
        project.tasks.getByName('buildRpmFoobarDaemon')
        project.tasks.getByName('buildDebBazDaemon')
        project.tasks.getByName('buildRpmBazDaemon')

    }
    def 'run tasks'() {
        when:
        project.plugins.apply(SystemPackagingPlugin)
        OspackageDaemonPlugin plugin = project.plugins.apply(OspackageDaemonPlugin)
        plugin.extension.daemon {
            daemonName = 'foobar'
            command = 'exit 0'
        }
        project.evaluate()

        then: 'task is created after evaluation'
        DaemonTemplateTask templateTask = project.tasks.getByName('buildDebFoobarDaemon')
        templateTask != null

        when:
        templateTask.template()

        then:
        File initd = new File(projectDir, 'build/daemon/Foobar/buildDeb/initd')
        initd.exists()
    }
}
