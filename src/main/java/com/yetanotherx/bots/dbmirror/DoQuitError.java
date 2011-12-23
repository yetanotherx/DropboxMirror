package com.yetanotherx.bots.dbmirror;

class DoQuitError extends Exception {
    private static final long serialVersionUID = 1L;

    public DoQuitError(String string) {
        super(string);
    }

    public DoQuitError() {
        super();
    }

}
