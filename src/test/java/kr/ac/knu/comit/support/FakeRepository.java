package kr.ac.knu.comit.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * HashMap 기반 인메모리 JpaRepository 추상 베이스.
 * 단순 CRUD만 구현하고, 집계·페이지네이션은 UnsupportedOperationException을 던진다.
 */
public abstract class FakeRepository<T> implements JpaRepository<T, Long> {

    protected final Map<Long, T> store = new LinkedHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    protected abstract Long getId(T entity);

    // ── Core CRUD ────────────────────────────────────────────────────────────

    @Override
    public <S extends T> S save(S entity) {
        if (getId(entity) == null) {
            ReflectionTestUtils.setField(entity, "id", idSequence.getAndIncrement());
        }
        store.put(getId(entity), entity);
        return entity;
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        entities.forEach(e -> result.add(save(e)));
        return result;
    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<T> findAllById(Iterable<Long> ids) {
        List<T> result = new ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(result::add));
        return result;
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public void delete(T entity) {
        store.remove(getId(entity));
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        ids.forEach(store::remove);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public void deleteAllInBatch(Iterable<T> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        deleteAllById(ids);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public void flush() {
    }

    @Override
    public T getReferenceById(Long id) {
        return findById(id).orElseThrow(() ->
                new jakarta.persistence.EntityNotFoundException("Entity not found: " + id));
    }

    @Override
    @Deprecated
    public T getById(Long id) {
        return getReferenceById(id);
    }

    @Override
    @Deprecated
    public T getOne(Long id) {
        return getReferenceById(id);
    }

    // ── 미지원 (Sort / Pageable / Example) ───────────────────────────────────

    @Override
    public List<T> findAll(Sort sort) {
        throw new UnsupportedOperationException("Fake does not support findAll(Sort)");
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Fake does not support findAll(Pageable)");
    }

    @Override
    public <S extends T> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("Fake does not support findOne(Example)");
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("Fake does not support findAll(Example)");
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("Fake does not support findAll(Example, Sort)");
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("Fake does not support findAll(Example, Pageable)");
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        throw new UnsupportedOperationException("Fake does not support count(Example)");
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("Fake does not support exists(Example)");
    }

    @Override
    public <S extends T, R> R findBy(
            Example<S> example,
            Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction
    ) {
        throw new UnsupportedOperationException("Fake does not support findBy");
    }
}
