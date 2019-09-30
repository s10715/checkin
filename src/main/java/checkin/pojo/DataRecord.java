package checkin.pojo;

import java.util.ArrayList;

public class DataRecord<T> extends ArrayList<T> implements Record {

private CellStyle cellStyle;

    public DataRecord addData(T... data) {
        for (T d : data)
            add(d);
        return this;
    }

    public void setCellStyle(CellStyle cellStyle) {
        this.cellStyle = cellStyle;
    }

    @Override
    public CellStyle getCellStyle() {
        return cellStyle;
    }
}
