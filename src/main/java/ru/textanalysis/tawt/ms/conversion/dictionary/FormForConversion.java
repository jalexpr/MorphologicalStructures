package ru.textanalysis.tawt.ms.conversion.dictionary;

import ru.textanalysis.tawt.ms.grammeme.MorfologyParameters;
import ru.textanalysis.tawt.ms.grammeme.MorfologyParametersHelper;
import ru.textanalysis.tawt.ms.loader.BDFormString;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.textanalysis.tawt.ms.loader.LoadHelper.createKeyWithControlCode;
import static ru.textanalysis.tawt.ms.loader.LoadHelper.getHashCode;
import static template.wrapper.conversion.Bytes.getBytes;
import static template.wrapper.conversion.Bytes.plusByte;

public class FormForConversion {

    private static final Map<String, Integer> STRING_INTEGER_WORD_FORM_MAP = new HashMap<>();
    private static final Map<String, Integer> STRING_INTEGER_INITIAL_FORM_MAP = new HashMap<>();
    private static final int START_ID_INITIAL_FORM = BDFormString.START_ID_INITIAL_FORM;
    private static final int START_ID_WORD_FORM = BDFormString.START_ID_WORD_FORM;

    private String stringName;
    private int key;
    private byte partOfSpeech;
    private byte[] morfCharacteristics;
    private boolean isExistInBd;

    protected FormForConversion(String stringName, boolean isInitialForm) {
        this.stringName = stringName.toLowerCase();
        key = createKey(isInitialForm);
    }

    protected String getStringName() {
        return stringName;
    }

    protected void setCharacteristics(String[] characteristics) {
        List<String> parameters = new ArrayList<>(Arrays.asList(characteristics));
        setPartOfSpeech(conversionPartOfSpeech(parameters));
        setMorfCharacteristics(getBytes(conversionMorfCharacteristics(parameters)));
    }

    private void setPartOfSpeech(byte partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    private byte getPartOfSpeech() {
        return partOfSpeech;
    }

    private static byte conversionPartOfSpeech(List<String> parameters) {
        byte partOfSpeech;
        int indexEnd = parameters.size() - 1;
        try {
            partOfSpeech = MorfologyParametersHelper.getTypeOfSpeech(parameters.get(indexEnd));
            parameters.remove(indexEnd);
            if(indexEnd != 0) {
                parameters.remove(0);
            }
            return partOfSpeech;
        } catch (Exception ex) {
            try {
                partOfSpeech = MorfologyParametersHelper.getTypeOfSpeech(parameters.get(0));
                parameters.remove(0);
                if(partOfSpeech == MorfologyParameters.TypeOfSpeech.NUMERAL && parameters.size() > 0 && parameters.get(0).equals("coll")) {
                    partOfSpeech = MorfologyParametersHelper.getTypeOfSpeech(parameters.get(0));
                    parameters.remove(0);
                }
                if(partOfSpeech == MorfologyParameters.TypeOfSpeech.ADJECTIVEFULL || partOfSpeech == MorfologyParameters.TypeOfSpeech.NOUNPRONOUN) {
                    if(parameters.size() > 3 && parameters.get(3).equals("anph")) {
                        partOfSpeech = MorfologyParametersHelper.getTypeOfSpeech(parameters.get(3));
                        parameters.remove(3);
                    }
                    if(parameters.size() > 2 && parameters.get(2).equals("anph")) {
                        partOfSpeech = MorfologyParametersHelper.getTypeOfSpeech(parameters.get(2));
                        parameters.remove(2);
                    }
                    if(parameters.size() > 1 && parameters.get(1).equals("anph")) {
                        partOfSpeech = MorfologyParametersHelper.getTypeOfSpeech(parameters.get(1));
                        parameters.remove(1);
                    }
                    if(parameters.size() > 0 && parameters.get(0).equals("anph")) {
                        partOfSpeech = MorfologyParametersHelper.getTypeOfSpeech(parameters.get(0));
                        parameters.remove(0);
                    }
                }
                return partOfSpeech;
            } catch (Exception exc) {
                String messages = String.format("Часть речи не найдена: %s и %s", parameters.get(0), parameters.get(parameters.size() - 1));
                Logger.getLogger(FormForConversion.class.getName()).log(Level.SEVERE, messages, exc);
                return 0;
            }
        }
    }

    private static long conversionMorfCharacteristics(List<String> parameters) {
        long numberParameters = 0;
        for(String parameter : parameters) {
            try {
                numberParameters |= MorfologyParametersHelper.getParameter(parameter);
            } catch (Exception ex){
                String messages = "Характеристика не найдена: " + parameter;
                Logger.getLogger(FormForConversion.class.getName()).log(Level.SEVERE, messages, ex);
            }
        }
        return numberParameters;
    }

    private void setMorfCharacteristics(byte[] morfCharacteristics) {
        this.morfCharacteristics = morfCharacteristics;
    }

    private byte[] getMorfCharacteristics() {
        return morfCharacteristics;
    }

    public int getKey() {
        return key;
    }

    protected boolean isExistInBd() {
        return isExistInBd;
    }

    private int createKey(boolean isInitialForm) {
        if(isInitialForm) {
            return createKey(STRING_INTEGER_INITIAL_FORM_MAP, START_ID_INITIAL_FORM);
        } else {
            return createKey(STRING_INTEGER_WORD_FORM_MAP, START_ID_WORD_FORM);
        }
    }

    private int createKey(Map<String, Integer> stringFormMap, int startKey) {
        if(stringFormMap.containsKey(getStringName())) {
            isExistInBd = true;
            return stringFormMap.get(getStringName());
        } else {
            int newKey = 0;
            try {
                isExistInBd = false;
                newKey = createKeyWithControlCode(startKey, stringFormMap.size(), getStringName());
                stringFormMap.put(getStringName(), newKey);
            } catch (Exception ex) {
                Logger.getLogger(FormForConversion.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
            return newKey;
        }
    }

    protected boolean isInitialForm() {
        return getKey() < START_ID_WORD_FORM;
    }

    protected byte[] getByteFileFormat() {
        byte[] hashCode = getBytes(getHashCode(getStringName()));
        byte[] bytesFormat = plusByte(hashCode, getBytes(getKey()));
        if(isInitialForm()) {
            bytesFormat = plusByte(bytesFormat, getPartOfSpeech());
        }
        bytesFormat = plusByte(bytesFormat, getMorfCharacteristics());
        return bytesFormat;
    }

    @Override
    public int hashCode() {
        return getHashCode(getStringName());
    }

    @Override
    public boolean equals(Object obj) {
        return getStringName().equals(((FormForConversion)obj).getStringName());
    }

}
