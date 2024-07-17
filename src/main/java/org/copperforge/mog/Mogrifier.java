package org.copperforge.mog;

import java.lang.reflect.Field;

import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.config.MogConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mogrifier {

    private Logger log = LoggerFactory.getLogger(Mogrifier.class);
    private MogConfig config = Mog.mog().config();
    private MogServiceManager manager;

    public Mogrifier() throws MogException {
        manager = MogServiceManager.instance();
    }

    public Mogrifier(MogConfig config) throws MogException {
        this.config = config;
        manager = MogServiceManager.instance();
    }

    public Mogrifier config(MogConfig config) {
        this.config = config;
        return this;
    }

    public Mogrifier mogrify(Object moggable) throws MogException {
        log.debug("Transmogrifying :: " + moggable.getClass());
        try {

            Field[] fields = moggable.getClass().getDeclaredFields();
            for (Field field : fields) {
                Moglet[] moglets = field.getAnnotationsByType(Moglet.class);
                for (Moglet moglet : moglets) {
                    field.setAccessible(true);
                    Class<?> mogletClass = field.getType();
                    log.debug("mogletClass :: " + mogletClass);

                    if (mogletClass.equals(MogConfig.class)) {
                        field.set(moggable, config);
                    } else if (moglet.name().isEmpty()) {
                        if (mogletClass.equals(Object.class)) {
                            mogletClass = field.getType();
                        }
                        field.set(moggable, manager.get(mogletClass));
                    } else {
                        field.set(moggable, manager.get(moglet.name()));
                    }
                }

            }
            return this;
        } catch (Exception e) {
            log.error("Exception during transomogrification of " + moggable.getClass().getName(), e);
            throw new MogException("Error during transmogrification", e);
        }
    }

}
