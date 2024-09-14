package org.copperforge.mog.conversion.dotout;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOParser {

    private static final String FIELD_REGEX="(?<name>[a-zA-Z0-9_]+)\\s+at\\s+(?<start>\\d+)\\s+for\\s+(?<length>\\d+)\\s+type\\s+(?<type>[a-zA-Z]+)[\\((?<integral>\\d+)[,(?<precision>\\\\d+)]*\\)]*\\s+(?<nullable>NULL|NOTNULL)*";
    private static final Pattern FIELD_PATTERN = Pattern.compile(FIELD_REGEX);

    public DOFileFormat parse(final String outFile) throws DOException {
        try {
            DOFileFormat format = new DOFileFormat();
            format.setOutFile(outFile);
            format.setFields(parseFile(outFile));
            return format;
        } catch (Exception e) {
            throw new DOException(e);
        }
    }

    private List<DOField> parseFile(String outFile) throws IOException {
        List<DOField> fields = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(outFile));
        String line = reader.readLine();
        while (line != null) {

            if (line.matches(FIELD_REGEX)) {
                fields.add(parseField(line));
            }
            
            line = reader.readLine();
        }

        reader.close();

        return fields;
    }

    private DOField parseField(String line) {
        DOField field = new DOField();
        Matcher m = FIELD_PATTERN.matcher(line);
        if (m.matches()) {
            field.setName(m.group("name"));
            field.setStartPos(Integer.parseInt(m.group("start")));
            field.setLength(Integer.parseInt(m.group("length")));
            field.setType(parseFieldType(m.group("type")));
            if (m.namedGroups().containsKey("integral")) field.setIntegral(Integer.parseInt(m.group("integral")));
            if (m.namedGroups().containsKey("precision")) field.setPrecision(Integer.parseInt(m.group("precision")));

            if (m.group("nullable") != null) {
                if (m.group("nullable").equals("NOTNULL")) field.setNullable(false);
            }
        }

        return field;
    }

    private DOFieldType parseFieldType(String type) {
        return DOFieldType.valueOf(type);
    }
}
