package com.example.satunetra_v3.data;

import com.example.satunetra_v3.model.Feel;
import com.example.satunetra_v3.model.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TagMaps {
    Map<String, Feel> tagMap;

    public TagMaps() {
        tagMap = new HashMap<>();
    }

    public Map<String, Feel> readTags(){
        HashMap<String, Instruction> m1 = new HashMap<String, Instruction>()
        {{
            put("b01", new Instruction("musik terapi", new ArrayList<>()));
            put("b02", new Instruction("suara alam", new ArrayList<>()));
            put("b03", new Instruction("audio meditasi", new ArrayList<>()));
        }};
        Feel val1 = new Feel(m1, "depresi", new String[]{"b01", "b02", "b03"});
        tagMap.put("a01", val1);

        HashMap<String, Instruction> m2 = new HashMap<String, Instruction>()
        {{
            put("b04", new Instruction("musik terapi", new ArrayList<>()));
            put("b05", new Instruction("suara alam", new ArrayList<>()));
            put("b06", new Instruction("audio meditasi", new ArrayList<>()));
        }};
        Feel val2 = new Feel(m2, "cemas", new String[]{"b04", "b05", "b06" });
        tagMap.put("a02", val2);

        HashMap<String, Instruction> m3 = new HashMap<String, Instruction>()
        {{
            put("b07", new Instruction("suara alam", new ArrayList<>()));
            put("b08", new Instruction("audio meditasi", new ArrayList<>()));
        }};
        Feel val3 = new Feel(m3, "Stress pasca trauma", new String[]{"b07", "b08"});
        tagMap.put("a03", val3);

        return  tagMap;
    }
}
