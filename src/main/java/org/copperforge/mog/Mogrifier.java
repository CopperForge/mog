package org.copperforge.mog;

import java.lang.reflect.Field;

import org.copperforge.mog.annotations.Moglet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mogrifier {

    private static Logger log = LoggerFactory.getLogger(Mog.class);

    public static void mogrify(Object moggable) throws MogException {
        log.trace("Transmogrifying :: " + moggable.getClass());
        try {

            Field[] fields = moggable.getClass().getDeclaredFields();
            for (Field field : fields) {
                Moglet[] moglets = field.getAnnotationsByType(Moglet.class);
                for (Moglet moglet : moglets) {
                    if (moglet.name().isEmpty()) {
                        Class<?> mogletClass = moglet.type();
                        if (mogletClass.equals(Object.class)) {
                            mogletClass = field.getType();
                        }
                        field.set(moggable, MogServiceManager.instance().get(mogletClass));
                    } else {
                        field.set(moggable, MogServiceManager.instance().get(moglet.name()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception during transomogrification of " + moggable.getClass().getName(), e);
            throw new MogException("Error during transmogrification", e);
        }
    }
    
}
