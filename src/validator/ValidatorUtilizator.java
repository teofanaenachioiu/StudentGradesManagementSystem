package validator;

import domain.Utilizator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtilizator implements Validator<Utilizator> {
    private static Pattern usrPtrn = Pattern.compile("^[A-Za-z0-9_.]+$");


    @Override
    public void validate(Utilizator entity) throws ValidationException {
        Matcher mtch = usrPtrn.matcher(entity.getID());
        if(!mtch.matches()){
            throw new ValidationException("Utilizator incorect!");
        }
    }
}
