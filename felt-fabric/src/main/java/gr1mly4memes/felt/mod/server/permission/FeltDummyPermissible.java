package gr1mly4memes.felt.mod.server.permission;

import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;

public class FeltDummyPermissible extends PermissibleBase {

    public static final ServerOperator DUMMY_OPERATOR = new ServerOperator() {
        @Override
        public boolean isOp() {
            return false;
        }

        @Override
        public void setOp(boolean b) {}
    };
    public FeltDummyPermissible() {
        super(DUMMY_OPERATOR);
    }
}
