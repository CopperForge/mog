package org.copperforge.mog.archiving;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.command.MogCommandResponse;
import org.copperforge.mog.io.MogFileFinder;
import org.copperforge.mog.io.MogPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// "archiveSets": [
//     {
//             "name": "conversion-environment",
//             "assets": [
//                     "/conv/bin",
//                     "/conv/etc",
//                     "/conv/include",
//                     "/conv/lib",
//                     "/conv/local",
//                     "/conv/opt",
//                     "/conv/prod",
//                     "/conv/share",
//                     "/conv/src",
//                     "/conv/var"
//             ],
//             "exclude": [
//                     "/conv/var/contexts/*",
//                     "**/.git"
//             ]
//     },
//     {
//             "name": "server-environment",
//             "assets": [
//                     "/etc/nginx",
//                     "/etc/systemd/system/gitea.service",
//                     "/etc/systemd/system/jenkins.service",
//                     "/etc/systemd/system/jenkins.service.d",
//                     "/etc/systemd/system/nginx.service.d",
//                     "/etc/systemd/system/otto.service",
//                     "/opt/oracle"
//             ],
//             "exclude": [ "**.git" ]
//     },
//     {
//             "name": "homes",
//             "assets": [
//                     "/home/PR/**/develop",
//                     "/home/dsprconv"
//             ],
//             "exclude": [ "**.git" ]
//     }
// ],

@MogCommand(name = "archive", description = "Archiving tool using defined archive sets")
public class MogArchiver {

    private Logger log = LoggerFactory.getLogger(MogArchiver.class);

    @MogCommand(name = "deflate", description = "Deflate the given archive set")
    public MogCommandResponse deflate() throws MogException {
        MogCommandResponse response = new MogCommandResponse();

        log.info("Archive sets = " + Mog.mog().config().getArchiveSets());

        MogArchiveSet archiveSet = Mog.mog().config().getArchiveSets().get(0);
        List<File> assets = new ArrayList<>();

        // search assets for files
        for (String assetPath : archiveSet.getAssets()) {
            File f = new File(assetPath);
            if (!f.exists()) {
                log.warn("Asset '" + assetPath + "' does not exist");

                // check to see if it is a pattern
                MogPath path = new MogPath(assetPath);
                log.info("" + path);
                assets.addAll(new MogFileFinder(path.getFilename()).find(path.getBase()));
            } else if (f.isDirectory()) {
                log.info("Found directory '" + f.getAbsolutePath() + "'");
                assets.addAll(getDirectoryAssets(f.getAbsolutePath()));
            } else {
                log.info("Found file '" + f.getAbsolutePath() + "'");
                assets.add(f);
            }
        }
        log.info("Assets = " + assets);

        // match and remove exclusions
        PathMatcher matcher;
        List<File> exclusions = new ArrayList<>();
        for (File asset : assets) {
            for (String exclusion : archiveSet.getExclusions()) {
                matcher = FileSystems.getDefault().getPathMatcher("glob:" + exclusion);
                if (matcher.matches(asset.toPath())) {
                    log.info("File '" + asset.getAbsolutePath() + "' matches exclusion '" + exclusion + "'");
                    exclusions.add(asset);
                }
            }
        }

        assets.removeAll(exclusions);
        log.info("Assets (final) = " + assets);

        return response;
    }

    private List<File> getDirectoryAssets(String absolutePath) {
        List<File> assets = new ArrayList<>();
        File d = new File(absolutePath);
        for (File f : d.listFiles()) {
            if (f.isDirectory()) {
                assets.addAll(getDirectoryAssets(f.getAbsolutePath()));
            } else {
                assets.add(f);
            }
        }
        return assets;
    }

    @MogCommand(name = "inflate", description = "Inflate the given archive set")
    public MogCommandResponse inflate() {
        MogCommandResponse response = new MogCommandResponse();

        return response;
    }

}
