package Services.interfaces;

import Models.TopActivite;

import java.util.List;

public interface IGenericService<T> {

    void add(T t) throws Exception;

    void update(T t) throws Exception;

    void delete(int id) throws Exception;

    List<T> getAll() throws Exception;

    T getById(int id) throws Exception;

    List<TopActivite> getTop3Activites();


}