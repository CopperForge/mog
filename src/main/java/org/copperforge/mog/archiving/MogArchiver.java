package org.copperforge.mog.archiving;

import java.io.File;
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
        MogFileFinder fileFinder = new MogFileFinder();

        log.info("Archive sets = " + Mog.mog().config().getArchiveSets());

        MogArchiveSet archiveSet = Mog.mog().config().getArchiveSets().get(0);
        List<File> assets = new ArrayList<>();

        // search assets for files
        for (String assetPath : archiveSet.getAssets()) {
            File f = new File(assetPath);
            if (!f.exists()) {
                // check to see if it is a pattern
                MogPath path = new MogPath(assetPath);
                log.info("" + path);
                assets.addAll(fileFinder.listFiles(path.getBase(), path.getFilename()));
            } else if (f.isDirectory()) {
                log.info("Found directory '" + f.getAbsolutePath() + "'");
                assets.addAll(fileFinder.listFiles(f.getAbsolutePath()));
            } else {
                log.info("Found file '" + f.getAbsolutePath() + "'");
                assets.add(f);
            }
        }
        log.info("Assets = " + assets);

        // match and remove exclusions
        List<File> exclusions = new ArrayList<>();
        for (String exclusion : archiveSet.getExclusions()) {
            exclusions.addAll(fileFinder.matchFiles(assets, exclusion));
        }
        log.info("exclusions = " + exclusions);

        assets.removeAll(exclusions);
        log.info("Assets (final) = " + assets);

        return response;
    }

    @MogCommand(name = "inflate", description = "Inflate the given archive set")
    public MogCommandResponse inflate() {
        MogCommandResponse response = new MogCommandResponse();

        return response;
    }

}
