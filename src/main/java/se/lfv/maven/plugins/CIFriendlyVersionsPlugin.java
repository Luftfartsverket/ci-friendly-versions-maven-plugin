// Copyright © LFV
package se.lfv.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

@Mojo(name = "set-version-from-tag", defaultPhase = LifecyclePhase.PACKAGE)
public class CIFriendlyVersionsPlugin extends AbstractMojo {

	@Parameter(property = "propertyName", defaultValue = "revision")
	private String propertyName;

	private static final Pattern TAG_VERSION_PATTERN = Pattern.compile("refs/tags/(?:v)?((\\d+\\.\\d+\\.\\d+)(.*))");

	public void execute() throws MojoExecutionException {
		VersionInformation vi = getVersion();

		getLog().info("Setting property '" + propertyName + "' to: " + vi);
		System.setProperty(propertyName, vi.toString());
	}

	private VersionInformation getVersion() throws MojoExecutionException {
		try (Git git = Git.open(new File("."))) {

			Iterator<RevCommit> latestCommitIterator = git.log().setMaxCount(1).call().iterator();
			// no commits in repository
			if (!latestCommitIterator.hasNext()) {
				getLog().debug("No commits");

				return addSnapshotQualifier(new VersionInformation("0.0.1-0"));
			}

			// latest commit has version tag
			RevCommit latestCommit = latestCommitIterator.next();
			List<String> versionTags = getVersionedTagsForCommit(git, latestCommit);

			getLog().debug("Latest commit: " + latestCommit);
			getLog().debug("current branch: " + git.getRepository().getFullBranch());

			Optional<VersionInformation> ovi = findHighestVersion(versionTags);

			if (ovi.isPresent()) {
				VersionInformation vi = ovi.get();
				getLog().debug("tag ref MATCHES: " + vi);

				return vi;
			}

			if (versionTags.size() == 1) {
				Matcher tagVersionMatcher = TAG_VERSION_PATTERN.matcher(versionTags.get(0));
				if (tagVersionMatcher.matches()) {
					String version = tagVersionMatcher.group(1);
					VersionInformation vi = new VersionInformation(version);

					return vi;
				}

			}

			getLog().debug("\n\nstart looking for version tagged commit");

			Iterable<RevCommit> commits = git.log().call();
			int count = 1;
			for (RevCommit commit : commits) {
				count++;
				versionTags = getVersionedTagsForCommit(git, commit);

				ovi = findHighestVersion(versionTags);

				if (ovi.isPresent()) {
					VersionInformation vi = ovi.get();

					vi.setPatch(vi.getPatch() + 1);
					vi.setBuildNumber(vi.getBuildNumber() + 1);

					return addSnapshotQualifier(vi);
				}
			}

			// no version tags in repository
			return addSnapshotQualifier(new VersionInformation("0.0.1-" + count));

		}
		catch (IOException | GitAPIException e) {
			throw new MojoExecutionException("Error reading Git information", e);
		}
	}

	private static Optional<VersionInformation> findHighestVersion(List<String> versionTags) {
		Optional<String> highestVersionString = versionTags.stream().max(new VersionComparator());

		return highestVersionString.map(VersionInformation::new);
	}

	private List<String> getVersionedTagsForCommit(Git git, RevCommit commit) throws GitAPIException {
		// get tags directly associated with the commit
		List<String> tagNames = git.tagList()
			.call()
			.stream()
			.filter(tag -> tag.getObjectId().equals(commit.getId()))
			.map(Ref::getName)
			.filter(tagName -> {
				Matcher matcher = TAG_VERSION_PATTERN.matcher(tagName);
				return matcher.matches() && matcher.groupCount() > 0;
			})
			.map(tagName -> {
				Matcher matcher = TAG_VERSION_PATTERN.matcher(tagName);
				matcher.matches();
				return matcher.group(1);
			})
			.collect(Collectors.toList());

		getLog().debug("VERSIONED Tags directly on commit " + commit.getName() + ": " + tagNames);

		return tagNames;
	}

	static VersionInformation addSnapshotQualifier(VersionInformation vi) {
		vi.setQualifier("SNAPSHOT");

		return vi;
	}

	static class VersionComparator implements Comparator<String> {

		@Override
		public int compare(String version1, String version2) {
			return new ComparableVersion(version1).compareTo(new ComparableVersion(version2));
		}

	}

}