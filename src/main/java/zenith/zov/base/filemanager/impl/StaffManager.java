package zenith.zov.base.filemanager.impl;

import com.google.common.reflect.TypeToken;
import zenith.zov.base.filemanager.api.ManagerFileAbstract;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class StaffManager extends ManagerFileAbstract<String> {

    public StaffManager() {
        super("staffName.json", "",  new TypeToken<Set<String>>() {}.getType(), HashSet::new);

    }
    public boolean isStaff(String staffName) {
        return getItems().contains(staffName);
    }

}
