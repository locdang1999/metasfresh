#!/usr/bin/env groovy
// the "!#/usr/bin... is just to to help IDEs, GitHub diffs, etc properly detect the language and do syntax highlighting for you.
// thx to https://github.com/jenkinsci/pipeline-examples/blob/master/docs/BEST_PRACTICES.md

// note that we set a default version for this library in jenkins, so we don't have to specify it here
@Library('misc')
import de.metas.jenkins.DockerConf
import de.metas.jenkins.Misc
import de.metas.jenkins.MvnConf

def build(final MvnConf mvnConf, final Map scmVars)
{
    stage('Build misc services')
    {
	currentBuild.description= """${currentBuild.description}<p/>
			<h2>misc services</h2>
		"""

		dir('edi')
		{
			def ediBuildFile = load('buildfile.groovy')
			ediBuildFile.build(mvnConf, scmVars)
		}
		dir('procurement-webui')
		{
			def procurementWebuiBuildFile = load('buildfile.groovy')
			procurementWebuiBuildFile.build(mvnConf, scmVars)
		}
		dir('admin')
		{
			def procurementWebuiBuildFile = load('buildfile.groovy')
			procurementWebuiBuildFile.build(mvnConf, scmVars)
		}
    } // stage
} 

return this