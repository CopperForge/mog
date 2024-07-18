package org.copperforge.mog.archiving;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

        log.debug("Archive sets = " + Mog.mog().config().getArchiveSets());

        MogArchiveSet archiveSet = Mog.mog().config().getArchiveSets().get(0);
        List<File> assets = new ArrayList<>();

        // search assets for files
        for (String assetPath : archiveSet.getAssets()) {
            File f = new File(assetPath);
            if (!f.exists()) {
                // check to see if it is a pattern
                MogPath path = new MogPath(assetPath);
                log.debug("" + path);
                assets.addAll(fileFinder.listFiles(path.getBase(), path.getFilename()));
            } else if (f.isDirectory()) {
                log.debug("Found directory '" + f.getAbsolutePath() + "'");
                assets.addAll(fileFinder.listFiles(f.getAbsolutePath()));
            } else {
                log.debug("Found file '" + f.getAbsolutePath() + "'");
                assets.add(f);
            }
        }
        log.debug("Assets = " + assets);

        // match and remove exclusions
        List<File> exclusions = new ArrayList<>();
        for (String exclusion : archiveSet.getExclusions()) {
            exclusions.addAll(fileFinder.matchFiles(assets, exclusion));
        }
        log.debug("exclusions = " + exclusions);

        assets.removeAll(exclusions);
        log.debug("Assets (final) = " + assets);

        String archiveName = "archive.zip";
        try {
            final FileOutputStream fos = new FileOutputStream(archiveName);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            String zipEntryName;
            int firstIndex;
            for (File srcFile : assets) {
                firstIndex = 0;
                zipEntryName = srcFile.getAbsolutePath();
                if (zipEntryName.contains("/") && zipEntryName.contains("\\")) {
                    firstIndex = zipEntryName.indexOf("/") > zipEntryName.indexOf("\\") ? zipEntryName.indexOf("\\")
                            : zipEntryName.indexOf("/");
                } else if (zipEntryName.contains("/")) {
                    firstIndex = zipEntryName.indexOf("/");
                } else if (zipEntryName.contains("\\")) {
                    firstIndex = zipEntryName.indexOf("\\");
                }
                zipEntryName = zipEntryName.substring(firstIndex+1);

                log.info("Adding " + zipEntryName + " ...");

                FileInputStream fis = new FileInputStream(srcFile);
                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            }

            zipOut.close();
            fos.close();
        } catch (IOException e) {
            log.error("Unable to create archive " + archiveName, e);
            response.setReturnCode(-1);
            response.setResponse(new ByteArrayInputStream(e.getLocalizedMessage().getBytes()));
        }

        return response;
    }

    @SuppressWarnings("resource")
    @MogCommand(name = "inflate", description = "Inflate the given archive set")
    public MogCommandResponse inflate() {
        MogCommandResponse response = new MogCommandResponse();
        String archiveName = "archive.zip";

        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(archiveName));
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

        } catch (IOException e) {
            log.error("Unable to extract archive " + archiveName, e);
            response.setReturnCode(-1);
            response.setResponse(new ByteArrayInputStream(e.getLocalizedMessage().getBytes()));
        }
        return response;
    }

}
