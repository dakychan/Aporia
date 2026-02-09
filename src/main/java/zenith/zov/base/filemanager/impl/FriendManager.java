package zenith.zov.base.filemanager.impl;

import com.google.common.reflect.TypeToken;
import zenith.zov.base.filemanager.api.ManagerFileAbstract;

import java.util.HashSet;
import java.util.Set;

public class FriendManager extends ManagerFileAbstract<String> {
    public FriendManager() {
        super("friends.json", "",  new TypeToken<Set<String>>() {}.getType(), HashSet::new);

    }

    public boolean isFriend(String name) {
      return this.getItems().contains(name);
    }
    public boolean removeFriend(String name) {
        return this.getItems().remove(name);
    }

}









