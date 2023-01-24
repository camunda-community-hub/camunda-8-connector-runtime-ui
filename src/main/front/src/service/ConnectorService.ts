import store, { AppThunk } from '../store';
import { loadStart, loadSuccess, setCurrent, fail } from '../store/features/connectors/slice';
import api from './api';

export class ConnectorService {
  lastFetch: number = 0;
  getDefaultForm = ():any => {
    return {
      name: 'New Connector',
      jobType: 'io.camunda:template:1',
      fetchVariables: ['processVar1'],
      jarFile: '',
      started: false
    }
  }
  getConnectors = (): AppThunk => async dispatch => {
    if (this.lastFetch < Date.now() - 5000) { 
      try {
        dispatch(loadStart());
        const { data } = await api.get<any[]>('/connectors');
        dispatch(loadSuccess(data));
      } catch(error: any) {
        if (error.response) {
          // The request was made. server responded out of range of 2xx
          dispatch(fail(error.response.data.message));
        } else if (error.request) {
          // The request was made but no response was received
          dispatch(fail('ERROR_NETWORK'));
        } else {
          // Something happened in setting up the request that triggered an Error
          console.warn('Error', error.message);
          dispatch(fail(error.toString()));
        }
      }
      this.lastFetch = Date.now();
    }
  }
  clone = (): any => {
    return JSON.parse(JSON.stringify(store.getState().connectors.current));
  }
  new = (): AppThunk => async dispatch => {
    dispatch(setCurrent(this.getDefaultForm()));
  }
  open = (name:string): AppThunk => async dispatch => {
    api.get('/connectors/' + name).then(response => {
      let form = response.data;
      form.previewData = JSON.stringify(form.previewData, null, 2);

      dispatch(setCurrent(form));
    }).catch(error => {
      alert(error.message);
    })
  }
  start = (name: string): AppThunk => async dispatch => {
    api.get('/connectors/start/' + name).then(response => {
      dispatch(this.getConnectors());
    }).catch(error => {
      alert(error.message);
    })
  }
  stop = (name: string): AppThunk => async dispatch => {
    api.get('/connectors/stop/' + name).then(response => {
      dispatch(this.getConnectors());
    }).catch(error => {
      alert(error.message);
    })
  }
  delete = (name: string): AppThunk => async dispatch => {
    api.delete('/connectors/' + name).then(response => {
      dispatch(this.getConnectors());
    }).catch(error => {
      alert(error.message);
    })
  }
  setCurrent = (connector: any): AppThunk => async dispatch => {
    dispatch(setCurrent(connector));
  }
  save = (): AppThunk => async dispatch => {
    let connector = this.clone();
    api.post('/connectors', connector).then(response => {
      connector.modified = response.data.modified;
      dispatch(setCurrent(connector));
    }).catch(error => {
      alert(error.message);
    })
    //this.$store.form.previewData = JSON.stringify(this.$store.form.previewData, null, 2);
  }
  uploadJar = (file: any): AppThunk => async dispatch => {
    
    const formData = new FormData();
    formData.append('File', file);
    let connector = this.clone();

    api.post('/connectors/upload', formData).then(response => {
      connector.modified = response.data.modified;
      connector.jarFile = response.data.jarFile;
      if (!connector.name) {
        connector.name = response.data.name;
      }
      dispatch(setCurrent(connector));
    }).catch(error => {
      alert(error.message);
    })
  }
}

const connectorService = new ConnectorService();

export default connectorService;