package com.floweytf.fma.features.cz.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class CharmDataRegistry<T> {
   final Map<String, T> byName = new Object2ObjectLinkedOpenHashMap();

   public Optional<T> byName(String name) {
      return Optional.ofNullable(this.byName.get(name));
   }

   public Map<String, T> getMap() {
      return Collections.unmodifiableMap(this.byName);
   }
}
