package service;

import domain.Nota;
import domain.Student;
import domain.Tema;
import javafx.util.Pair;
import repository.Repository;
import utils.*;
import validator.ValidationException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static utils.Config.filterAndSorter;


public class TemaService extends AbstractService<String, Tema> {
    Repository<String, Student> repoS;
    Repository<Pair<String,String>, Nota> repoN;

    public TemaService(Repository<String, Tema> repository,Repository<String, Student> repoS,Repository<Pair<String,String>, Nota> repoN) {
        super(repository);
        this.repoS=repoS;
        this.repoN=repoN;
    }


    public Tema modificareDeadline(String id, String deadlineVechiString, String deadlineNouString) throws ValidationException{
        if(utils.Config.getWeekUni()==null)throw new ValidationException("Nu se mai pot modifica deadline-urile!");
        Tema tema=find(id);
        Tema returned=tema;
        try {
            if(tema!=null) {
                int deadlineVechi = Integer.parseInt(deadlineVechiString);
                if (deadlineVechi < utils.Config.getWeekUni())
                    throw new ValidationException("S-a depasit deadline-ul vechi!");

                if(Integer.parseInt(deadlineNouString)<utils.Config.getWeekUni())
                    throw new ValidationException("Nu ma pot intoarece in timp...");

                int dataPredare = Integer.parseInt(tema.getDataPredare());
                int deadline = Integer.parseInt(tema.getDeadline());
                if (dataPredare > deadline) {
                    throw new ValidationException("Deadline-ul e mai mic fata de data de predare a temei!");
                }
                tema.setDeadline(deadlineNouString);
            }
            returned=update(tema);
            if (returned == null) notifyObservers(new DataChanged(EventType.UPDATE));
        }
        catch (ValidationException e){
            GUIUtils.showErrorMessage(e.getMessage());
        }
        return returned;
    }

    public boolean modificDeadline(Tema tema, String deadlineVechiString, String deadlineNouString) throws ValidationException{
        boolean modific=false;
        if(utils.Config.getWeekUni()==null)throw new ValidationException("Nu se mai pot modifica deadline-urile!");
        try {
                if (Integer.parseInt(deadlineVechiString) < utils.Config.getWeekUni())
                    throw new ValidationException("S-a depasit deadline-ul vechi!");
                if(Integer.parseInt(deadlineNouString)<utils.Config.getWeekUni())
                    throw new ValidationException("Nu ma pot intoarece in timp...");
                int dataPredare = Integer.parseInt(tema.getDataPredare());
                int deadline = Integer.parseInt(tema.getDeadline());
                if (dataPredare > deadline)
                    throw new ValidationException("Deadline-ul e mai mic fata de data de predare a temei!");
                tema.setDeadline(deadlineNouString);
                modific=true;
        }
        catch (ValidationException e){
            GUIUtils.showErrorMessage(e.getMessage());
        }
        return modific;
    }

    @Override
    public Tema add(Tema entity) throws ValidationException {
        Tema returned=entity;
        try{
            returned=super.add(entity);
            if(returned!=null)
                GUIUtils.showErrorMessage("ID existent!");
            else {
                notifyObservers(new DataChanged(EventType.ADD));
                SendEmail email=new SendEmail("teofanaenachioiu@yahoo.com",
                        "Dragi studenti,\n\n"
                                +"Tocmai a fost adaugata o noua tema de laborator. \n"
                                +"Informatii tema: \n"
                                +"NR TEMA: "+entity.getID()+".\n "
                                +"DESCRIERE TEMA: "+entity.getDescriere()+"\n"
                                +"DEADLINE: "+entity.getDeadline()+"\n"
                                +"DATA PREDARII: "+entity.getDataPredare()
                                +"\n\n"+
                                "Pe curand!",
                        "Adaugare tema"
                );
                email.run();
            }
        }
        catch (ValidationException e){
            GUIUtils.showErrorMessage(e.getMessage());
        }
        return returned;
    }

    @Override
    public Tema remove(String s) {
        Tema returned=super.remove(s);
        if(returned!=null){
            stergeNote(s);
            notifyObservers(new DataChanged(EventType.DELETE));
        }
        return returned;
    }

    @Override
    public void removeAll() {
        super.removeAll();
        notifyObservers(new DataChanged(EventType.DELETE));
    }

    @Override
    public Tema update(Tema entity) throws ValidationException {
        Tema tema=find(entity.getID());
        boolean modific=false;
        if(tema!=null){
            if(entity.getDeadline().equals("")) entity.setDeadline(tema.getDeadline());
            if(entity.getDescriere().equals("")) entity.setDescriere(tema.getDescriere());
            if(!tema.getDeadline().equals(entity.getDeadline())) {
                modific=modificDeadline(entity, tema.getDeadline(), entity.getDeadline());
            }
            if(!tema.getDescriere().equals(entity.getDescriere()))
                modific=true;
            if(entity.getDataPredare().equals("")) entity.setDataPredare(tema.getDataPredare());
        }
        Tema returned=entity;
        if(modific) {
            returned=super.update(entity);
        }
        if(returned==null){
            notifyObservers(new DataChanged(EventType.UPDATE));
            SendEmail email=new SendEmail("teofanaenachioiu@yahoo.com",
                    "Dragi studenti,\n\n"
                    +"Tocmai s-a modificat tema de laborator numarul "+entity.getID()+".\n "
                            +"Datele actualizate sunt: \n"
                            +"DESCRIERE TEMA: "+entity.getDescriere()+"\n"
                            +"DEADLINE: "+entity.getDeadline()+"\n"
                            +"DATA PREDARII: "+entity.getDataPredare()
                            +"\n\n"+
                            "Pe curand!",
                    "Modificare tema"
                    );
            email.run();
        }

        return returned;
    }

    public List<Tema> filtreazaTemaDeadline(String deadline) {
        return filterAndSorter(getAll(), entity -> entity.getDeadline().equals(deadline), Comparator.comparing(Tema::getDeadline));
    }

    public List<Tema> sorteazaTemaDeadline() {
        return filterAndSorter(getAll(), null, Comparator.comparing(Tema::getDeadline));
    }

    public List<Tema> filtreazaTemaKeyword(String keyword) {
        return filterAndSorter(getAll(),
                entity -> entity.getDescriere().toLowerCase().contains(keyword.toLowerCase()) ||
                        String.valueOf(entity.getDeadline()).contains(keyword),
                Comparator.comparing(Tema::getID));
    }

    private void stergeNote(String idTema){
        for(Student student:repoS.findAll()){
            Pair<String, String> id=new Pair(student.getID(),idTema);
            if(repoN.findOne(Optional.ofNullable(id)).isPresent())
                repoN.delete(Optional.of(id));
        }
    }


}
