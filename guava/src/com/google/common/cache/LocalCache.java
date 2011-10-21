/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;
import com.google.common.cache.LocalCacheAsMap.Segment;
import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Exposes a {@link LocalCacheAsMap} as a {@code Cache}.
 *
 * @author Charles Fry
 */
class LocalCache<K, V> extends AbstractCache<K, V> implements Serializable {
  final LocalCacheAsMap<K, V> map;

  LocalCache(CacheBuilder<? super K, ? super V> builder,
      Supplier<? extends StatsCounter> statsCounterSupplier,
      CacheLoader<? super K, V> loader) {
    this.map = new LocalCacheAsMap<K, V>(builder, statsCounterSupplier, loader);
  }

  // Cache methods

  @Override
  public V get(K key) throws ExecutionException {
    return map.getOrLoad(key);
  }

  @Override
  public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
    return map.getOrLoadAll(keys);
  }

  @Override
  public V get(K key, final Callable<V> valueLoader) throws ExecutionException {
    checkNotNull(valueLoader);
    return map.getOrLoad(key, new CacheLoader<Object, V>() {
      @Override
      public V load(Object key) throws Exception {
        return valueLoader.call();
      }
    });
  }

  @Override
  public void refresh(K key) throws ExecutionException {
    map.refresh(key);
  }

  @Override
  public void invalidate(Object key) {
    checkNotNull(key);
    map.remove(key);
  }

  @Override
  public void invalidateAll(Iterable<?> keys) {
    map.invalidateAll(keys);
  }

  @Override
  public void invalidateAll() {
    map.clear();
  }

  @Override
  public long size() {
    return map.longSize();
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return map;
  }

  @Override
  public CacheStats stats() {
    SimpleStatsCounter aggregator = new SimpleStatsCounter();
    aggregator.incrementBy(map.globalStatsCounter);
    for (Segment<K, V> segment : map.segments) {
      aggregator.incrementBy(segment.statsCounter);
    }
    return aggregator.snapshot();
  }

  @Override
  public void cleanUp() {
    map.cleanUp();
  }

  // Serialization Support

  private static final long serialVersionUID = 1;

  Object writeReplace() {
    return map.cacheSerializationProxy();
  }

}